package com.library.audit;

import com.library.dto.AuditLogDTO;
import com.library.entity.AuditLog;
import com.library.entity.enums.AuditOutcome;
import com.library.labeling.OutputLabelService;
import com.library.labeling.OutputLabelService.OutputLabel;
import com.library.repository.AuditLogRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Read side of the audit trail: filtered reports and labeled CSV export for administrators. */
@Service
@Transactional(readOnly = true)
public class AuditQueryService {

    /** Upper bound for one export, to keep a single request from loading the whole table. */
    static final int MAX_EXPORT_ROWS = 10_000;

    private static final Set<Character> FORMULA_PREFIXES = Set.of('=', '+', '-', '@', '\t', '\r');

    private final AuditLogRepository repository;
    private final OutputLabelService labelService;

    public AuditQueryService(AuditLogRepository repository, OutputLabelService labelService) {
        this.repository = repository;
        this.labelService = labelService;
    }

    public record Filter(String action, AuditOutcome outcome, String search,
                         LocalDateTime from, LocalDateTime to) {
    }

    public Page<AuditLogDTO> search(Filter filter, Pageable pageable) {
        return repository.findAll(toSpec(filter), pageable).map(AuditLogDTO::from);
    }

    /** CSV with the output label (classification, requester, IP, time) as its first line. */
    public byte[] exportCsv(Filter filter) {
        OutputLabel label = labelService.labelFor(labelService.systemClassification());
        List<AuditLog> rows = repository.findAll(toSpec(filter),
                PageRequest.of(0, MAX_EXPORT_ROWS, Sort.by("id").descending())).getContent();

        StringBuilder csv = new StringBuilder("﻿"); // BOM so spreadsheet tools read UTF-8 Persian text
        csv.append(cell(label.toPersianLine())).append('\n');
        csv.append("id,timestamp,actorId,actorEmail,action,outcome,entityType,entityId,ipAddress,userAgent,details\n");
        for (AuditLog r : rows) {
            csv.append(String.join(",",
                    cell(r.getId()), cell(r.getTimestamp()), cell(r.getActorId()), cell(r.getActorEmail()),
                    cell(r.getAction()), cell(r.getOutcome()), cell(r.getEntityType()), cell(r.getEntityId()),
                    cell(r.getIpAddress()), cell(r.getUserAgent()), cell(r.getDetails()))).append('\n');
        }
        csv.append(cell(label.toPersianLine())).append('\n');
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static Specification<AuditLog> toSpec(Filter f) {
        return (root, query, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            if (f.action() != null && !f.action().isBlank()) ps.add(cb.equal(root.get("action"), f.action()));
            if (f.outcome() != null) ps.add(cb.equal(root.get("outcome"), f.outcome()));
            if (f.from() != null) ps.add(cb.greaterThanOrEqualTo(root.get("timestamp"), f.from()));
            if (f.to() != null) ps.add(cb.lessThanOrEqualTo(root.get("timestamp"), f.to()));
            if (f.search() != null && !f.search().isBlank()) {
                String pattern = "%" + f.search().trim().toLowerCase() + "%";
                ps.add(cb.or(
                        cb.like(cb.lower(cb.coalesce(root.get("actorEmail"), "")), pattern),
                        cb.like(cb.lower(cb.coalesce(root.get("ipAddress"), "")), pattern),
                        cb.like(cb.lower(cb.coalesce(root.get("details"), "")), pattern)));
            }
            return cb.and(ps.toArray(new Predicate[0]));
        };
    }

    /** RFC 4180 quoting, plus neutralising spreadsheet formulas (CSV injection). */
    private static String cell(Object value) {
        if (value == null) return "";
        String s = value.toString();
        if (!s.isEmpty() && FORMULA_PREFIXES.contains(s.charAt(0))) {
            s = "'" + s;
        }
        return "\"" + s.replace("\"", "\"\"") + "\"";
    }
}
