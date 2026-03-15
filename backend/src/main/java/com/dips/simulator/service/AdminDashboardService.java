package com.dips.simulator.service;

import com.dips.simulator.domain.TransactionEntity;
import com.dips.simulator.domain.enums.TransactionState;
import com.dips.simulator.dto.AdminDashboardResponse;
import com.dips.simulator.dto.AdminDashboardTransactionResponse;
import com.dips.simulator.repository.TransactionRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AdminDashboardService {

    private static final int MAX_LIMIT = 100;

    private final TransactionRepository transactionRepository;

    public AdminDashboardService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    public AdminDashboardResponse dashboard(int limit, String userId) {
        if (limit < 1 || limit > MAX_LIMIT) {
            throw new DomainException("limit must be between 1 and " + MAX_LIMIT);
        }

        String normalizedUserId = userId == null ? null : userId.trim();
        boolean filtered = normalizedUserId != null && !normalizedUserId.isBlank();

        List<TransactionEntity> allTransactions = transactionRepository.findAll();
        List<TransactionEntity> scope = filtered
                ? allTransactions.stream()
                .filter(tx -> normalizedUserId.equals(tx.getPayerVpa()) || normalizedUserId.equals(tx.getPayeeVpa()))
                .toList()
                : allTransactions;

        long totalTransactions = scope.size();
        long completed = scope.stream().filter(tx -> tx.getState() == TransactionState.COMPLETED).count();
        long reversed = scope.stream().filter(tx -> tx.getState() == TransactionState.REVERSED).count();
        long failedPreDebit = scope.stream().filter(tx -> tx.getState() == TransactionState.FAILED_PRE_DEBIT).count();
        OffsetDateTime last24hThreshold = OffsetDateTime.now().minusHours(24);
        long last24h = filtered
                ? scope.stream().filter(tx -> tx.getCreatedAt().isAfter(last24hThreshold)).count()
                : transactionRepository.countByCreatedAtAfter(last24hThreshold);

        BigDecimal initiatedAmount = scope.stream()
                .map(TransactionEntity::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal completedAmount = scope.stream()
                .filter(tx -> tx.getState() == TransactionState.COMPLETED)
                .map(TransactionEntity::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal successRatePercent = totalTransactions == 0
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(completed)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(totalTransactions), 2, RoundingMode.HALF_UP);

        Map<TransactionState, Long> byState = new EnumMap<>(TransactionState.class);
        scope.forEach(tx -> byState.merge(tx.getState(), 1L, Long::sum));

        List<TransactionEntity> recentEntities = filteredTransactions(limit, normalizedUserId);
        List<AdminDashboardTransactionResponse> recent = recentEntities.stream()
                .map(this::toRecentTransaction)
                .toList();

        AdminDashboardResponse response = new AdminDashboardResponse();
        response.setGeneratedAt(OffsetDateTime.now());
        response.setTotalTransactions(totalTransactions);
        response.setTransactionsLast24Hours(last24h);
        response.setCompletedTransactions(completed);
        response.setReversedTransactions(reversed);
        response.setFailedPreDebitTransactions(failedPreDebit);
        response.setSuccessRatePercent(successRatePercent);
        response.setTotalInitiatedAmount(initiatedAmount);
        response.setCompletedAmount(completedAmount);
        response.setStateBreakdown(
                byState.entrySet().stream().collect(Collectors.toMap(
                        entry -> entry.getKey().name(),
                        Map.Entry::getValue
                ))
        );
        response.setRecentTransactions(recent);

        // --- KPI CHARTS: last 7 days ---
        // Prepare time series for tx volume, error rate, GMV
        java.time.LocalDate today = java.time.LocalDate.now();
        java.util.List<String> labels = new java.util.ArrayList<>();
        java.util.List<Long> txVolume = new java.util.ArrayList<>();
        java.util.List<Long> errorRate = new java.util.ArrayList<>();
        java.util.List<java.math.BigDecimal> gmv = new java.util.ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            java.time.LocalDate day = today.minusDays(i);
            labels.add(day.toString());
            long txCount = scope.stream().filter(tx -> tx.getCreatedAt().toLocalDate().equals(day)).count();
            txVolume.add(txCount);
            long errorCount = scope.stream().filter(tx -> tx.getCreatedAt().toLocalDate().equals(day) &&
                (tx.getState() == com.dips.simulator.domain.enums.TransactionState.FAILED_PRE_DEBIT ||
                 tx.getState() == com.dips.simulator.domain.enums.TransactionState.REVERSED)).count();
            errorRate.add(txCount == 0 ? 0 : Math.round((errorCount * 100.0) / txCount));
            java.math.BigDecimal dayGmv = scope.stream()
                .filter(tx -> tx.getCreatedAt().toLocalDate().equals(day) && tx.getState() == com.dips.simulator.domain.enums.TransactionState.COMPLETED)
                .map(TransactionEntity::getAmount)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
            gmv.add(dayGmv);
        }
        java.util.Map<String, Object> kpiCharts = new java.util.HashMap<>();
        kpiCharts.put("txVolume", java.util.Map.of("labels", labels, "values", txVolume));
        kpiCharts.put("errorRate", java.util.Map.of("labels", labels, "values", errorRate));
        kpiCharts.put("gmv", java.util.Map.of("labels", labels, "values", gmv));
        response.setKpiCharts(kpiCharts);
        return response;
    }

    private List<TransactionEntity> filteredTransactions(int limit, String userId) {
        PageRequest page = PageRequest.of(0, limit);
        if (userId == null || userId.isBlank()) {
            return transactionRepository.findAllByOrderByCreatedAtDesc(page).getContent();
        }
        String normalized = userId.trim();
        return transactionRepository.findByPayerVpaOrPayeeVpaOrderByCreatedAtDesc(normalized, normalized, page).getContent();
    }

    private AdminDashboardTransactionResponse toRecentTransaction(TransactionEntity tx) {
        AdminDashboardTransactionResponse row = new AdminDashboardTransactionResponse();
        row.setTransactionId(tx.getId());
        row.setClientRequestId(tx.getClientRequestId());
        row.setPayerVpa(tx.getPayerVpa());
        row.setPayeeVpa(tx.getPayeeVpa());
        row.setAmount(tx.getAmount());
        row.setSource(tx.getSource());
        row.setState(tx.getState());
        row.setCreatedAt(tx.getCreatedAt());
        row.setUpdatedAt(tx.getUpdatedAt());
        row.setProcessingTimeMs(Duration.between(tx.getCreatedAt(), tx.getUpdatedAt()).toMillis());
        return row;
    }
}
