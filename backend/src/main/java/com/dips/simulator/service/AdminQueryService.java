package com.dips.simulator.service;

import com.dips.simulator.domain.AccountEntity;
import com.dips.simulator.domain.UserEntity;
import com.dips.simulator.domain.enums.BankType;
import com.dips.simulator.dto.AdminAccountResponse;
import com.dips.simulator.repository.AccountRepository;
import com.dips.simulator.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AdminQueryService {

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;

    public AdminQueryService(AccountRepository accountRepository, UserRepository userRepository) {
        this.accountRepository = accountRepository;
        this.userRepository = userRepository;
    }

    public List<AdminAccountResponse> listAccounts(String userVpa, BankType bankType) {
        return accountRepository.findAll().stream()
                .filter(acc -> userVpa == null || userVpa.isBlank() || acc.getVpa().equalsIgnoreCase(userVpa.trim()))
                .filter(acc -> bankType == null || acc.getBankType() == bankType)
                .map(this::toResponse)
                .toList();
    }

    private AdminAccountResponse toResponse(AccountEntity account) {
        UserEntity user = userRepository.findByVpa(account.getVpa()).orElse(null);
        AdminAccountResponse response = new AdminAccountResponse();
        response.setVpa(account.getVpa());
        response.setDisplayName(user == null ? "Unknown" : user.getDisplayName());
        response.setBankNodeCode(user == null ? null : user.getBankNodeCode());
        response.setBankType(account.getBankType());
        response.setBalance(account.getBalance());
        return response;
    }
}

