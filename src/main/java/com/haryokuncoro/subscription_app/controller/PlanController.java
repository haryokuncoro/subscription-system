package com.haryokuncoro.subscription_app.controller;

import com.haryokuncoro.subscription_app.dto.ApiResponse;
import com.haryokuncoro.subscription_app.dto.PlanRequest;
import com.haryokuncoro.subscription_app.dto.PlanResponse;
import com.haryokuncoro.subscription_app.service.PlanService;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/plans")
@RequiredArgsConstructor
public class PlanController {

    private final PlanService planService;

    @GetMapping
    public ApiResponse<List<PlanResponse>> getAll() {

        return ApiResponse.success(planService.findAll());
    }

    @GetMapping("/{id}")
    public ApiResponse<PlanResponse> getById(@PathVariable UUID id) {

        return ApiResponse.success(planService.findById(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<PlanResponse> create(@Valid @RequestBody PlanRequest request) {

        return ApiResponse.success(
                "Plan created successfully",
                planService.create(request)
        );
    }

    @PutMapping("/{id}")
    public ApiResponse<PlanResponse> update(@PathVariable UUID id, @Valid @RequestBody PlanRequest request) {

        return ApiResponse.success(
                "Plan updated successfully",
                planService.update(id, request)
        );
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable UUID id) {

        planService.delete(id);

        return ApiResponse.success("Plan deleted successfully");
    }

}