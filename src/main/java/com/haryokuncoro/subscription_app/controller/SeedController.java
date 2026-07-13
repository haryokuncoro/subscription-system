package com.haryokuncoro.subscription_app.controller;

import com.haryokuncoro.subscription_app.dto.ApiResponse;
import com.haryokuncoro.subscription_app.service.SeedService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/seed")
@RequiredArgsConstructor
public class SeedController {

    private final SeedService seedService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Void> seedBasicData() {
        seedService.seedData();
        return ApiResponse.success(
                "seeded users, plans, subscriptions successfully"
        );
    }

    @PostMapping("/invoices")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Void> seedInvoice() {
        seedService.seedInvoice();
        return ApiResponse.success(
                "seeded invoices successfully"
        );
    }

}
