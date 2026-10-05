package com.library.audit;

import com.library.BaseIntegrationTest;
import com.library.entity.AuditLog;
import com.library.entity.enums.AuditAction;
import com.library.entity.enums.AuditOutcome;
import com.library.repository.AuditLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Audit trail: recording and tamper-evident hash chain")
class AuditServiceTest extends BaseIntegrationTest {

    @Autowired private AuditService auditService;
    @Autowired private AuditLogRepository repository;
    @Autowired private JdbcTemplate jdbc;

    @BeforeEach
    void clean() {
        repository.deleteAll();
    }

    @Test
    @DisplayName("Records carry action, outcome, actor and are chained to the previous record")
    void recordsAreChained() {
        auditService.record(AuditEntry.success(AuditAction.LOGIN).actorId(7L).actorEmail("a@x.ir").build());
        auditService.record(AuditEntry.failure(AuditAction.LOGIN).actorEmail("b@x.ir").details("BadCredentials").build());

        List<AuditLog> rows = repository.findAll();
        assertThat(rows).hasSize(2);
        AuditLog first = rows.get(0);
        AuditLog second = rows.get(1);
        assertThat(first.getOutcome()).isEqualTo(AuditOutcome.SUCCESS);
        assertThat(first.getActorId()).isEqualTo(7L);
        assertThat(first.getTimestamp()).isNotNull();
        assertThat(first.getPrevHash()).isNull();
        assertThat(second.getPrevHash()).isEqualTo(first.getRecordHash());
        assertThat(second.getOutcome()).isEqualTo(AuditOutcome.FAILURE);
    }

    @Test
    @DisplayName("An intact chain verifies")
    void intactChainVerifies() {
        for (int i = 0; i < 5; i++) {
            auditService.record(AuditEntry.success(AuditAction.API_WRITE).details("call " + i).build());
        }
        AuditService.ChainVerification result = auditService.verifyChain();
        assertThat(result.valid()).isTrue();
        assertThat(result.recordsChecked()).isEqualTo(5);
    }

    @Test
    @DisplayName("Editing a stored record is detected")
    void modifiedRecordDetected() {
        auditService.record(AuditEntry.success(AuditAction.LOGIN).actorEmail("a@x.ir").build());
        auditService.record(AuditEntry.failure(AuditAction.ACCESS_DENIED).actorEmail("a@x.ir").build());
        auditService.record(AuditEntry.success(AuditAction.LOGOUT).actorEmail("a@x.ir").build());
        Long tamperedId = repository.findAll().get(1).getId();

        jdbc.update("UPDATE audit_logs SET outcome = 'SUCCESS' WHERE id = ?", tamperedId);

        AuditService.ChainVerification result = auditService.verifyChain();
        assertThat(result.valid()).isFalse();
        assertThat(result.firstInvalidId()).isEqualTo(tamperedId);
    }

    @Test
    @DisplayName("Deleting a record from the middle is detected")
    void deletedRecordDetected() {
        auditService.record(AuditEntry.success(AuditAction.LOGIN).build());
        auditService.record(AuditEntry.success(AuditAction.API_WRITE).build());
        auditService.record(AuditEntry.success(AuditAction.LOGOUT).build());
        List<AuditLog> rows = repository.findAll();

        jdbc.update("DELETE FROM audit_logs WHERE id = ?", rows.get(1).getId());

        AuditService.ChainVerification result = auditService.verifyChain();
        assertThat(result.valid()).isFalse();
        assertThat(result.firstInvalidId()).isEqualTo(rows.get(2).getId());
    }
}
