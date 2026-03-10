package com.dips.simulator.dto;

import com.dips.simulator.domain.enums.BankType;

import java.math.BigDecimal;

public class AdminAccountResponse {

    private String vpa;
    private String displayName;
    private String bankNodeCode;
    private BankType bankType;
    private BigDecimal balance;

    public String getVpa() {
        return vpa;
    }

    public void setVpa(String vpa) {
        this.vpa = vpa;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getBankNodeCode() {
        return bankNodeCode;
    }

    public void setBankNodeCode(String bankNodeCode) {
        this.bankNodeCode = bankNodeCode;
    }

    public BankType getBankType() {
        return bankType;
    }

    public void setBankType(BankType bankType) {
        this.bankType = bankType;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }
}

