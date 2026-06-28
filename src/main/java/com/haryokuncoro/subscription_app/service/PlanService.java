package com.haryokuncoro.subscription_app.service;

import com.haryokuncoro.subscription_app.dto.PlanRequest;
import com.haryokuncoro.subscription_app.dto.PlanResponse;
import com.haryokuncoro.subscription_app.dto.StripePlanResult;
import com.haryokuncoro.subscription_app.entity.Plan;
import com.haryokuncoro.subscription_app.exception.NotFoundException;
import com.haryokuncoro.subscription_app.repository.PlanRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor @Slf4j
@Transactional(readOnly = true)
public class PlanService {

    private final PlanRepository planRepository;
    private final StripeService stripeService;

    public List<PlanResponse> findAll() {

        return planRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public PlanResponse findById(UUID id) {

        Plan plan = planRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Plan not found"));

        return this.toResponse(plan);
    }

    @Transactional
    public PlanResponse create(PlanRequest request) {
        StripePlanResult stripeResult = stripeService.createProductAndPrice(request);

        Plan plan = Plan.builder()
                .name(request.getName())
                .description(request.getDescription())
                .stripeProductId(stripeResult.productId())
                .stripePriceId(stripeResult.priceId())
                .amount(request.getAmount())
                .currency(request.getCurrency().toUpperCase())
                .country(request.getCountry().toUpperCase())
                .billingInterval(request.getBillingInterval())
                .active(request.getActive())
                .build();

        planRepository.save(plan);
        return this.toResponse(plan);
    }

    @Transactional
    public PlanResponse update(UUID id, PlanRequest request) {

        Plan plan = planRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Plan not found"));
        StripePlanResult stripeResult = stripeService.updateProductAndPrice(plan, request);

        plan.setName(request.getName());
        plan.setDescription(request.getDescription());
        plan.setStripePriceId(stripeResult.priceId());
        plan.setAmount(request.getAmount());
        plan.setCurrency(request.getCurrency().toUpperCase());
        plan.setCountry(request.getCountry().toUpperCase());
        plan.setBillingInterval(request.getBillingInterval());
        plan.setActive(request.getActive());
        return this.toResponse(plan);
    }

    @Transactional
    public void deactivate(UUID id) {
        Plan plan = planRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Plan not found"));
        plan.setActive(false);
        planRepository.save(plan);
        stripeService.archiveProductAndPrice(plan);
    }

    public PlanResponse toResponse(Plan plan) {

        return PlanResponse.builder()
                .id(plan.getId())
                .name(plan.getName())
                .description(plan.getDescription())
                .stripeProductId(plan.getStripeProductId())
                .stripePriceId(plan.getStripePriceId())
                .amount(plan.getAmount())
                .currency(plan.getCurrency())
                .country(plan.getCountry())
                .billingInterval(plan.getBillingInterval())
                .active(plan.isActive())
                .createdAt(plan.getCreatedAt())
                .updatedAt(plan.getUpdatedAt())
                .build();
    }
}
