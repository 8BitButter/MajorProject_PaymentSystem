package com.dips.simulator.controller;

import com.dips.simulator.dto.OfflineSmsRequest;
import com.dips.simulator.dto.OfflineSmsResponse;
import com.dips.simulator.service.OfflineSmsService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/offline/sms")
public class OfflineSmsController {

    private final OfflineSmsService offlineSmsService;

    public OfflineSmsController(OfflineSmsService offlineSmsService) {
        this.offlineSmsService = offlineSmsService;
    }

    @PostMapping("/submit")
    public OfflineSmsResponse submit(@Valid @RequestBody OfflineSmsRequest request) {
        return offlineSmsService.handle(request);
    }
}

