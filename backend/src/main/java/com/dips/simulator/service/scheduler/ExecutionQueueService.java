package com.dips.simulator.service.scheduler;

import com.dips.simulator.domain.ExecutionQueueEntity;
import com.dips.simulator.domain.enums.ExecutionQueueStatus;
import com.dips.simulator.repository.ExecutionQueueRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class ExecutionQueueService {

    private static final Logger log = LoggerFactory.getLogger(ExecutionQueueService.class);

    private final ExecutionQueueRepository queueRepository;
    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final DataSource dataSource;
    private final String dbSchema;

    private boolean postgresDialect = false;
    private boolean queueTableAvailable = true;
    private String queueTableRef = "execution_queue";
    private boolean missingQueueLogged = false;

    public ExecutionQueueService(
            ExecutionQueueRepository queueRepository,
            NamedParameterJdbcTemplate jdbcTemplate,
            DataSource dataSource,
            @Value("${DIPS_DB_SCHEMA:dips_app}") String dbSchema
    ) {
        this.queueRepository = queueRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.dataSource = dataSource;
        this.dbSchema = dbSchema;
    }

    @PostConstruct
    void detectDialect() {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            String product = metaData.getDatabaseProductName();
            String driver = metaData.getDriverName();
            boolean h2Driver = driver != null && driver.toLowerCase().contains("h2");
            postgresDialect = !h2Driver && product != null && product.toLowerCase().contains("postgresql");
            queueTableRef = postgresDialect ? qualify("execution_queue") : "execution_queue";
            queueTableAvailable = tableExists(connection, "execution_queue");
            if (!queueTableAvailable) {
                log.warn("Execution queue table not found (expected {}). Scheduler polling will be skipped until schema is migrated.", queueTableRef);
            }
        } catch (SQLException ignored) {
            postgresDialect = false;
            queueTableAvailable = false;
        }
    }

    @Transactional
    public void enqueue(UUID transactionId, BigDecimal amount, BigDecimal smallValueThreshold) {
        if (queueRepository.existsById(transactionId)) {
            return;
        }
        ExecutionQueueEntity entry = new ExecutionQueueEntity();
        entry.setTransactionId(transactionId);
        entry.setPriorityScore(amount.compareTo(smallValueThreshold) <= 0 ? 10 : 100);
        entry.setQueueStatus(ExecutionQueueStatus.PENDING);
        entry.setAttemptCount(0);
        entry.setNextAttemptAt(OffsetDateTime.now());
        entry.setCreatedAt(OffsetDateTime.now());
        entry.setUpdatedAt(OffsetDateTime.now());
        queueRepository.save(entry);
    }

    @Transactional
    public Optional<UUID> claimNext(String workerId) {
        if (!queueTableAvailable) {
            return Optional.empty();
        }

        if (!postgresDialect) {
            return claimNextPortable(workerId);
        }

        String sql = String.format("""
                WITH candidate AS (
                    SELECT transaction_id
                    FROM %s
                    WHERE queue_status = 'PENDING'
                      AND next_attempt_at <= NOW()
                    ORDER BY priority_score ASC, created_at ASC
                    FOR UPDATE SKIP LOCKED
                    LIMIT 1
                )
                UPDATE %s q
                SET queue_status = 'PROCESSING',
                    locked_by = :workerId,
                    locked_at = NOW(),
                    updated_at = NOW()
                FROM candidate
                WHERE q.transaction_id = candidate.transaction_id
                RETURNING q.transaction_id
                """, queueTableRef, queueTableRef);
        try {
            List<UUID> rows = jdbcTemplate.query(sql,
                    new MapSqlParameterSource("workerId", workerId),
                    (rs, rowNum) -> rs.getObject(1, UUID.class));
            if (!rows.isEmpty()) {
                return Optional.of(rows.get(0));
            }
            return claimNextPortable(workerId);
        } catch (BadSqlGrammarException ex) {
            handleMissingQueueTable(ex);
            return Optional.empty();
        }
    }

    @Transactional
    public void complete(UUID transactionId) {
        queueRepository.deleteById(transactionId);
    }

    @Transactional
    public void releasePending(UUID transactionId) {
        queueRepository.findById(transactionId).ifPresent(entry -> {
            entry.setQueueStatus(ExecutionQueueStatus.PENDING);
            entry.setLockedBy(null);
            entry.setLockedAt(null);
            entry.setLastErrorCode(null);
            entry.setUpdatedAt(OffsetDateTime.now());
            queueRepository.save(entry);
        });
    }

    @Transactional
    public void requeueAfterFailure(UUID transactionId, String errorCode, int maxRetryAttempts) {
        queueRepository.findById(transactionId).ifPresent(entry -> {
            int attempts = entry.getAttemptCount() + 1;
            entry.setAttemptCount(attempts);
            if (attempts >= maxRetryAttempts) {
                entry.setQueueStatus(ExecutionQueueStatus.DEAD_LETTER);
            } else {
                entry.setQueueStatus(ExecutionQueueStatus.PENDING);
                int delay = (int) Math.min(Math.pow(2, attempts), 60);
                entry.setNextAttemptAt(OffsetDateTime.now().plusSeconds(delay));
            }
            entry.setLockedBy(null);
            entry.setLockedAt(null);
            entry.setLastErrorCode(errorCode);
            entry.setUpdatedAt(OffsetDateTime.now());
            queueRepository.save(entry);
        });
    }

    public Map<String, Long> snapshot() {
        return Map.of(
                ExecutionQueueStatus.PENDING.name(), queueRepository.countByQueueStatus(ExecutionQueueStatus.PENDING),
                ExecutionQueueStatus.PROCESSING.name(), queueRepository.countByQueueStatus(ExecutionQueueStatus.PROCESSING),
                ExecutionQueueStatus.DEAD_LETTER.name(), queueRepository.countByQueueStatus(ExecutionQueueStatus.DEAD_LETTER)
        );
    }

    private Optional<UUID> claimNextPortable(String workerId) {
        String selectSql = String.format("""
                SELECT transaction_id
                FROM %s
                WHERE queue_status = 'PENDING'
                  AND next_attempt_at <= CURRENT_TIMESTAMP
                ORDER BY priority_score ASC, created_at ASC
                LIMIT 1
                """, queueTableRef);
        List<UUID> candidates;
        try {
            candidates = jdbcTemplate.query(selectSql, (rs, rowNum) -> rs.getObject(1, UUID.class));
        } catch (BadSqlGrammarException ex) {
            handleMissingQueueTable(ex);
            return Optional.empty();
        }
        if (candidates.isEmpty()) {
            return Optional.empty();
        }

        UUID candidate = candidates.get(0);
        String updateSql = String.format("""
                UPDATE %s
                SET queue_status = 'PROCESSING',
                    locked_by = :workerId,
                    locked_at = CURRENT_TIMESTAMP,
                    updated_at = CURRENT_TIMESTAMP
                WHERE transaction_id = :transactionId
                  AND queue_status = 'PENDING'
                """, queueTableRef);
        int updated = jdbcTemplate.update(updateSql, new MapSqlParameterSource()
                .addValue("workerId", workerId)
                .addValue("transactionId", candidate));
        return updated == 1 ? Optional.of(candidate) : Optional.empty();
    }

    private String qualify(String tableName) {
        if (dbSchema == null || dbSchema.isBlank()) {
            return tableName;
        }
        return dbSchema + "." + tableName;
    }

    private boolean tableExists(Connection connection, String tableName) throws SQLException {
        DatabaseMetaData metaData = connection.getMetaData();
        String schema = (dbSchema == null || dbSchema.isBlank()) ? null : dbSchema;
        if (existsInSchema(metaData, schema, tableName)) {
            return true;
        }
        return existsInSchema(metaData, "public", tableName);
    }

    private boolean existsInSchema(DatabaseMetaData metaData, String schema, String tableName) throws SQLException {
        try (ResultSet rs = metaData.getTables(null, schema, tableName, new String[]{"TABLE"})) {
            if (rs.next()) {
                return true;
            }
        }
        if (schema != null) {
            try (ResultSet rs = metaData.getTables(null, schema.toUpperCase(), tableName.toUpperCase(), new String[]{"TABLE"})) {
                return rs.next();
            }
        }
        return false;
    }

    private void handleMissingQueueTable(BadSqlGrammarException ex) {
        queueTableAvailable = false;
        if (!missingQueueLogged) {
            missingQueueLogged = true;
            log.error("Execution queue table is unavailable ({}). Apply latest Flyway migration and restart. Root cause: {}", queueTableRef, ex.getMostSpecificCause().getMessage());
        }
    }
}
