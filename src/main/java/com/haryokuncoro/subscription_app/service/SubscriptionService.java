package com.haryokuncoro.subscription_app.service;

import com.haryokuncoro.subscription_app.dto.SubscriptionRequest;
import com.haryokuncoro.subscription_app.dto.SubscriptionResponse;
import com.haryokuncoro.subscription_app.entity.Plan;
import com.haryokuncoro.subscription_app.entity.Subscription;
import com.haryokuncoro.subscription_app.entity.User;
import com.haryokuncoro.subscription_app.exception.NotFoundException;
import com.haryokuncoro.subscription_app.repository.PlanRepository;
import com.haryokuncoro.subscription_app.repository.SubscriptionRepository;
import com.haryokuncoro.subscription_app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service @RequiredArgsConstructor @Slf4j
@Transactional(readOnly = true)
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;
    private final PlanRepository planRepository;


    public List<SubscriptionResponse> findAll() {

        return subscriptionRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }


    public SubscriptionResponse findById(UUID id) {

        Subscription subscription = subscriptionRepository.findById(id)
                .orElseThrow(() ->
                        new NotFoundException("Subscription not found"));

        return this.toResponse(subscription);
    }

    @Transactional
    public SubscriptionResponse create(SubscriptionRequest request) {

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() ->
                        new NotFoundException("User not found"));

        Plan plan = planRepository.findById(request.getPlanId())
                .orElseThrow(() ->
                        new NotFoundException("Plan not found"));

        Subscription subscription = Subscription.builder()
                .user(user)
                .plan(plan)
                .status(request.getStatus())
                .currentPeriodStart(request.getCurrentPeriodStart())
                .currentPeriodEnd(request.getCurrentPeriodEnd())
                .cancelAtPeriodEnd(Boolean.TRUE.equals(request.getCancelAtPeriodEnd()))
                .build();

        subscriptionRepository.save(subscription);

        return this.toResponse(subscription);
    }

    @Transactional
    public SubscriptionResponse update(UUID id, SubscriptionRequest request) {

        Subscription subscription = subscriptionRepository.findById(id)
                .orElseThrow(() ->
                        new NotFoundException("Subscription not found"));

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() ->
                        new NotFoundException("User not found"));

        Plan plan = planRepository.findById(request.getPlanId())
                .orElseThrow(() ->
                        new NotFoundException("Plan not found"));

        subscription.setUser(user);
        subscription.setPlan(plan);
        subscription.setStatus(request.getStatus());
        subscription.setCurrentPeriodStart(request.getCurrentPeriodStart());
        subscription.setCurrentPeriodEnd(request.getCurrentPeriodEnd());
        subscription.setCancelAtPeriodEnd(
                Boolean.TRUE.equals(request.getCancelAtPeriodEnd()));

        return this.toResponse(subscription);
    }

    @Transactional
    public void delete(UUID id) {

        Subscription subscription = subscriptionRepository.findById(id)
                .orElseThrow(() ->
                        new NotFoundException("Subscription not found"));

        subscriptionRepository.delete(subscription);
    }

    public SubscriptionResponse toResponse(Subscription subscription) {

        return SubscriptionResponse.builder()
                .id(subscription.getId())
                .userId(subscription.getUser().getId())
                .userEmail(subscription.getUser().getEmail())
                .planId(subscription.getPlan().getId())
                .planName(subscription.getPlan().getName())
                .stripeSubscriptionId(subscription.getStripeSubscriptionId())
                .status(subscription.getStatus())
                .currentPeriodStart(subscription.getCurrentPeriodStart())
                .currentPeriodEnd(subscription.getCurrentPeriodEnd())
                .cancelAtPeriodEnd(subscription.isCancelAtPeriodEnd())
                .canceledAt(subscription.getCanceledAt())
                .createdAt(subscription.getCreatedAt())
                .updatedAt(subscription.getUpdatedAt())
                .build();
    }
}
