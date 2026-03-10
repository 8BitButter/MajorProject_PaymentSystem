package com.dips.simulator.controller;

import com.dips.simulator.domain.TransactionEntity;
import com.dips.simulator.dto.StageDecision;
import com.dips.simulator.repository.TransactionRepository;
import com.dips.simulator.service.AcquirerStageService;
import com.dips.simulator.service.DomainException;
import com.dips.simulator.service.IssuerStageService;
import com.dips.simulator.service.PspStageService;
import com.dips.simulator.service.SwitchStageService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/sim")
public class SimulationStageController {

    private final TransactionRepository transactionRepository;
    private final PspStageService pspStageService;
    private final SwitchStageService switchStageService;
    private final IssuerStageService issuerStageService;
    private final AcquirerStageService acquirerStageService;

    public SimulationStageController(
            TransactionRepository transactionRepository,
            PspStageService pspStageService,
            SwitchStageService switchStageService,
            IssuerStageService issuerStageService,
            AcquirerStageService acquirerStageService
    ) {
        this.transactionRepository = transactionRepository;
        this.pspStageService = pspStageService;
        this.switchStageService = switchStageService;
        this.issuerStageService = issuerStageService;
        this.acquirerStageService = acquirerStageService;
    }

    @PostMapping("/psp/validate/{txId}")
    public StageDecision validate(@PathVariable UUID txId) {
        return pspStageService.validateDecision(load(txId));
    }

    @PostMapping("/switch/route/{txId}")
    public StageDecision route(@PathVariable UUID txId) {
        return switchStageService.routeDecision(load(txId));
    }

    @PostMapping("/issuer/debit/{txId}")
    public StageDecision debit(@PathVariable UUID txId) {
        return issuerStageService.debitDecision(load(txId), false);
    }

    @PostMapping("/acquirer/credit/{txId}")
    public StageDecision credit(@PathVariable UUID txId) {
        return acquirerStageService.creditDecision(load(txId), false);
    }

    @PostMapping("/issuer/reversal/{txId}")
    public StageDecision reversal(@PathVariable UUID txId) {
        return issuerStageService.reversalDecision(load(txId), false);
    }

    private TransactionEntity load(UUID txId) {
        return transactionRepository.findById(txId)
                .orElseThrow(() -> new DomainException("Transaction not found: " + txId));
    }
}

