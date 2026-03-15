package com.dips.simulator.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

public class AdminDashboardResponse {

    private OffsetDateTime generatedAt;
    private long totalTransactions;
    private long transactionsLast24Hours;
    private long completedTransactions;
    private long reversedTransactions;
    private long failedPreDebitTransactions;
    private BigDecimal successRatePercent;
    private BigDecimal totalInitiatedAmount;
    private BigDecimal completedAmount;
    private Map<String, Long> stateBreakdown;
    private List<AdminDashboardTransactionResponse> recentTransactions;

    // KPI chart data for dashboard (time series)
    private Map<String, Object> kpiCharts;

    public Map<String, Object> getKpiCharts() {
        return kpiCharts;
    }

    public void setKpiCharts(Map<String, Object> kpiCharts) {
        this.kpiCharts = kpiCharts;
    }

    public OffsetDateTime getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(OffsetDateTime generatedAt) {
        this.generatedAt = generatedAt;
    }

    public long getTotalTransactions() {
        return totalTransactions;
    }

    public void setTotalTransactions(long totalTransactions) {
        this.totalTransactions = totalTransactions;
    }

    public long getTransactionsLast24Hours() {
        return transactionsLast24Hours;
    }

    public void setTransactionsLast24Hours(long transactionsLast24Hours) {
        this.transactionsLast24Hours = transactionsLast24Hours;
    }

    public long getCompletedTransactions() {
        return completedTransactions;
    }

    public void setCompletedTransactions(long completedTransactions) {
        this.completedTransactions = completedTransactions;
    }

    public long getReversedTransactions() {
        return reversedTransactions;
    }

    public void setReversedTransactions(long reversedTransactions) {
        this.reversedTransactions = reversedTransactions;
    }

    public long getFailedPreDebitTransactions() {
        return failedPreDebitTransactions;
    }

    public void setFailedPreDebitTransactions(long failedPreDebitTransactions) {
        this.failedPreDebitTransactions = failedPreDebitTransactions;
    }

    public BigDecimal getSuccessRatePercent() {
        return successRatePercent;
    }

    public void setSuccessRatePercent(BigDecimal successRatePercent) {
        this.successRatePercent = successRatePercent;
    }

    public BigDecimal getTotalInitiatedAmount() {
        return totalInitiatedAmount;
    }

    public void setTotalInitiatedAmount(BigDecimal totalInitiatedAmount) {
        this.totalInitiatedAmount = totalInitiatedAmount;
    }

    public BigDecimal getCompletedAmount() {
        return completedAmount;
    }

    public void setCompletedAmount(BigDecimal completedAmount) {
        this.completedAmount = completedAmount;
    }

    public Map<String, Long> getStateBreakdown() {
        return stateBreakdown;
    }

    public void setStateBreakdown(Map<String, Long> stateBreakdown) {
        this.stateBreakdown = stateBreakdown;
    }

    public List<AdminDashboardTransactionResponse> getRecentTransactions() {
        return recentTransactions;
    }

    public void setRecentTransactions(List<AdminDashboardTransactionResponse> recentTransactions) {
        this.recentTransactions = recentTransactions;
    }
}
