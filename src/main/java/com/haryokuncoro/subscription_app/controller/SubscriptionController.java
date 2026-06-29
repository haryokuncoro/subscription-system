package com.haryokuncoro.subscription_app.controller;

import com.haryokuncoro.subscription_app.dto.ApiResponse;
import com.haryokuncoro.subscription_app.dto.SubscriptionRequest;
import com.haryokuncoro.subscription_app.dto.SubscriptionResponse;
import com.haryokuncoro.subscription_app.service.SubscriptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    @GetMapping
    public ApiResponse<List<SubscriptionResponse>> getAll() {
        return ApiResponse.success(subscriptionService.findAll());
    }

    @GetMapping("/{id}")
    public ApiResponse<SubscriptionResponse> getById(@PathVariable UUID id) {

        return ApiResponse.success(subscriptionService.findById(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<SubscriptionResponse> create(@Valid @RequestBody SubscriptionRequest request) {

        return ApiResponse.success(
                "Subscription created successfully",
                subscriptionService.create(request)
        );
    }

    @PutMapping("/{id}")
    public ApiResponse<SubscriptionResponse> update(@PathVariable UUID id, @Valid @RequestBody SubscriptionRequest request) {

        return ApiResponse.success(
                "Subscription updated successfully",
                subscriptionService.update(id, request)
        );
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable UUID id, @RequestParam boolean immediately) {

        subscriptionService.cancel(id, immediately);

        return ApiResponse.success("Subscription deleted successfully");
    }

}