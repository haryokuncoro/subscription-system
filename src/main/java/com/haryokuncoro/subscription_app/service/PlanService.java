package com.haryokuncoro.subscription_app.service;

import com.haryokuncoro.subscription_app.dto.PlanRequest;
import com.haryokuncoro.subscription_app.dto.PlanResponse;
import com.haryokuncoro.subscription_app.dto.StripePlanResult;
import com.haryokuncoro.subscription_app.entity.Plan;
import com.haryokuncoro.subscription_app.exception.NotFoundException;
import com.haryokuncoro.subscription_app.repository.PlanRepository;
import com.haryokuncoro.subscription_app.utils.GeneralUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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
        Long amount = GeneralUtils.toCents(request.getAmount());
        Plan plan = Plan.builder()
                .name(request.getName())
                .description(request.getDescription())
                .stripeProductId(stripeResult.productId())
                .stripePriceId(stripeResult.priceId())
                .amount(amount)
                .currency(request.getCurrency().toUpperCase())
                .country(request.getCountry().toUpperCase())
                .billingInterval(request.getBillingInterval())
                .active(request.getActive())
                .build();

        plan = planRepository.save(plan);
        return this.toResponse(plan);
    }

    @Transactional
    public PlanResponse update(UUID id, PlanRequest request) {

        Plan plan = planRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Plan not found"));
        StripePlanResult stripeResult = stripeService.updateProductAndPrice(plan, request);
        Long amount = GeneralUtils.toCents(request.getAmount());
        plan.setName(request.getName());
        plan.setDescription(request.getDescription());
        plan.setStripePriceId(stripeResult.priceId());
        plan.setAmount(amount);
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
        BigDecimal amount = GeneralUtils.toDollars(plan.getAmount());
        return PlanResponse.builder()
                .id(plan.getId())
                .name(plan.getName())
                .description(plan.getDescription())
                .stripeProductId(plan.getStripeProductId())
                .stripePriceId(plan.getStripePriceId())
                .amount(amount)
                .currency(plan.getCurrency())
                .country(plan.getCountry())
                .billingInterval(plan.getBillingInterval())
                .active(plan.isActive())
                .createdAt(plan.getCreatedAt())
                .updatedAt(plan.getUpdatedAt())
                .build();
    }
}
