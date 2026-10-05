package com.library.audit;

import com.library.entity.AuditLog;
import com.library.repository.AuditLogRepository;
import com.library.util.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.Objects;

/**
 * Writes the security audit trail.
 *
 * <ul>
 *   <li>Each record is stored in its own transaction, so it survives a rollback of the
 *       business operation it describes.</li>
 *   <li>Records are hash-chained (SHA-256 over the previous hash plus the record's
 *       canonical fields); {@link #verifyChain()} detects any modified or removed row.</li>
 *   <li>Every record is mirrored to the "AUDIT" log stream (separate rolling file).</li>
 *   <li>Auditing never breaks the audited request: failures are logged, not thrown.</li>
 * </ul>
 * Appends are serialized in-process to keep the chain linear (single application instance).
 */
@Slf4j
@Service
public class AuditService {

    private static final Logger AUDIT_LOG = LoggerFactory.getLogger("AUDIT");
    private static final int USER_AGENT_MAX = 512;
    private static final int VERIFY_PAGE_SIZE = 500;

    private final AuditLogRepository repository;
    private final TransactionTemplate requiresNew;
    private final Object appendLock = new Object();

    public AuditService(AuditLogRepository repository, PlatformTransactionManager transactionManager) {
        this.repository = repository;
        this.requiresNew = new TransactionTemplate(transactionManager);
        this.requiresNew.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    public void record(AuditEntry entry) {
        try {
            AuditLog saved = append(toLog(entry));
            AUDIT_LOG.info("event={} outcome={} actorId={} actor={} entity={}:{} ip={} details={}",
                    saved.getAction(), saved.getOutcome(), saved.getActorId(), saved.getActorEmail(),
                    saved.getEntityType(), saved.getEntityId(), saved.getIpAddress(), saved.getDetails());
        } catch (Exception e) {
            log.error("Failed to write audit record for {}", entry.action(), e);
        }
    }

    /** Recomputes the whole hash chain; returns the id of the first broken record, if any. */
    public ChainVerification verifyChain() {
        String expectedPrev = null;
        long checked = 0;
        int page = 0;
        Page<AuditLog> batch;
        do {
            batch = repository.findAllByOrderByIdAsc(PageRequest.of(page++, VERIFY_PAGE_SIZE));
            for (AuditLog row : batch) {
                checked++;
                if (!Objects.equals(expectedPrev, row.getPrevHash())
                        || !Objects.equals(hash(row), row.getRecordHash())) {
                    return new ChainVerification(false, checked, row.getId());
                }
                expectedPrev = row.getRecordHash();
            }
        } while (batch.hasNext());
        return new ChainVerification(true, checked, null);
    }

    public record ChainVerification(boolean valid, long recordsChecked, Long firstInvalidId) {
    }

    private AuditLog append(AuditLog record) {
        synchronized (appendLock) {
            return requiresNew.execute(status -> {
                String prev = repository.findTopByOrderByIdDesc().map(AuditLog::getRecordHash).orElse(null);
                record.setPrevHash(prev);
                record.setRecordHash(hash(record));
                return repository.save(record);
            });
        }
    }

    private AuditLog toLog(AuditEntry entry) {
        HttpServletRequest request = currentRequest();
        Long actorId = entry.actorId() != null ? entry.actorId() : SecurityUtils.getCurrentUserId();
        String actorEmail = entry.actorEmail() != null ? entry.actorEmail() : currentEmail();
        return AuditLog.builder()
                .actorId(actorId)
                .actorEmail(actorEmail)
                .action(entry.action().name())
                .outcome(entry.outcome())
                .entityType(entry.entityType() != null ? entry.entityType() : "SYSTEM")
                .entityId(entry.entityId())
                .details(entry.details())
                .ipAddress(request != null ? request.getRemoteAddr() : null)
                .userAgent(request != null ? truncate(request.getHeader(HttpHeaders.USER_AGENT)) : null)
                // DB timestamps keep microseconds; truncate so the stored value re-hashes identically
                .timestamp(LocalDateTime.now().truncatedTo(ChronoUnit.MICROS))
                .build();
    }

    static String hash(AuditLog r) {
        String canonical = String.join("|",
                String.valueOf(r.getPrevHash()),
                String.valueOf(r.getTimestamp()),
                String.valueOf(r.getActorId()),
                String.valueOf(r.getActorEmail()),
                r.getAction(),
                String.valueOf(r.getOutcome()),
                r.getEntityType(),
                String.valueOf(r.getEntityId()),
                String.valueOf(r.getIpAddress()),
                String.valueOf(r.getUserAgent()),
                String.valueOf(r.getDetails()));
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    private static String currentEmail() {
        // Anonymous requests carry the principal name "anonymousUser"; record no actor instead
        return SecurityUtils.getCurrentUserId() != null ? SecurityUtils.getCurrentUserEmail() : null;
    }

    private static HttpServletRequest currentRequest() {
        return RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs
                ? attrs.getRequest()
                : null;
    }

    private static String truncate(String value) {
        return value == null || value.length() <= USER_AGENT_MAX ? value : value.substring(0, USER_AGENT_MAX);
    }
}
