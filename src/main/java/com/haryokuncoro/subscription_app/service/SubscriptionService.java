package com.haryokuncoro.subscription_app.service;

import com.haryokuncoro.subscription_app.dto.GetInvoiceResponse;
import com.haryokuncoro.subscription_app.dto.SubscriptionRequest;
import com.haryokuncoro.subscription_app.dto.SubscriptionResponse;
import com.haryokuncoro.subscription_app.dto.enums.SubscriptionStatus;
import com.haryokuncoro.subscription_app.dto.spec.InvoiceSpecification;
import com.haryokuncoro.subscription_app.dto.spec.SubscriptionSpecification;
import com.haryokuncoro.subscription_app.entity.Invoice;
import com.haryokuncoro.subscription_app.entity.Plan;
import com.haryokuncoro.subscription_app.entity.Subscription;
import com.haryokuncoro.subscription_app.entity.User;
import com.haryokuncoro.subscription_app.exception.NotFoundException;
import com.haryokuncoro.subscription_app.exception.StripeOperationException;
import com.haryokuncoro.subscription_app.repository.PlanRepository;
import com.haryokuncoro.subscription_app.repository.SubscriptionRepository;
import com.haryokuncoro.subscription_app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service @RequiredArgsConstructor @Slf4j
@Transactional(readOnly = true)
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;
    private final PlanRepository planRepository;
    private final StripeService stripeService;


    public Page<SubscriptionResponse> search(
            UUID userId,
            UUID planId,
            String status,
            Pageable pageable) {

        Specification<Subscription> spec = null;

        if (userId != null) {
            spec = Specification.allOf(
                    spec,
                    SubscriptionSpecification.hasUser(userId)
            );
        }

        if (planId != null) {
            spec = Specification.allOf(
                    spec,
                    SubscriptionSpecification.hasPlan(planId)
            );
        }

        if (status != null) {
            spec = Specification.allOf(
                    spec,
                    SubscriptionSpecification.hasStatus(status)
            );
        }

        return subscriptionRepository.findAll(spec, pageable)
                .map(this::toResponse);
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

        if (user.getStripeCustomerId() == null) {
            try {
                String stripeCustomerId = stripeService.createCustomer(user);
                user.setStripeCustomerId(stripeCustomerId);
                userRepository.save(user);
            } catch (Exception e) {
                throw new StripeOperationException("fail to create customer while creating subscription", e);
            }
        }

        com.stripe.model.Subscription stripeSubscription =
                stripeService.createSubscription(user.getStripeCustomerId(), plan.getCountry(), plan.getStripePriceId());

        Subscription subscription = Subscription.builder()
                .user(user)
                .plan(plan)
                .stripeSubscriptionId(stripeSubscription.getId())
                .status(mapStripeStatus(stripeSubscription.getStatus()))
                .currentPeriodStart(Instant.ofEpochSecond(stripeSubscription.getStartDate()))
                .currentPeriodEnd(Instant.ofEpochSecond(stripeSubscription.getEndedAt()))
                .cancelAtPeriodEnd(Boolean.TRUE.equals(stripeSubscription.getCancelAtPeriodEnd()))
                .build();

        subscriptionRepository.save(subscription);

        return this.toResponse(subscription);
    }

    private SubscriptionStatus mapStripeStatus(String stripeStatus) {
        return switch (stripeStatus) {
            case "active" -> SubscriptionStatus.ACTIVE;
            case "trialing" -> SubscriptionStatus.TRIALING;
            case "past_due" -> SubscriptionStatus.PAST_DUE;
            case "canceled" -> SubscriptionStatus.CANCELED;
            case "incomplete" -> SubscriptionStatus.INCOMPLETE;
            case "incomplete_expired" -> SubscriptionStatus.INCOMPLETE_EXPIRED;
            case "unpaid" -> SubscriptionStatus.UNPAID;
            default -> throw new IllegalArgumentException("Unknown Stripe status: " + stripeStatus);
        };
    }

    @Transactional
    public SubscriptionResponse update(UUID id, SubscriptionRequest request) {

        Subscription subscription = subscriptionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Subscription not found"));

        String newStripePriceId = null;
        Plan newPlan = subscription.getPlan();

        if (request.getPlanId() != null && !request.getPlanId().equals(subscription.getPlan().getId())) {
            newPlan = planRepository.findById(request.getPlanId())
                    .orElseThrow(() -> new NotFoundException("Plan not found"));
            newStripePriceId = newPlan.getStripePriceId();
        }

        com.stripe.model.Subscription stripeSubscription = stripeService.updateSubscription(
                subscription.getPlan().getCountry(),
                subscription.getStripeSubscriptionId(),
                newStripePriceId,
                request.getCancelAtPeriodEnd()
        );

        subscription.setPlan(newPlan);
        subscription.setStatus(mapStripeStatus(stripeSubscription.getStatus()));
        subscription.setCurrentPeriodStart(Instant.ofEpochSecond(stripeSubscription.getStartDate()));
        subscription.setCurrentPeriodEnd(Instant.ofEpochSecond(stripeSubscription.getEndedAt()));
        subscription.setCancelAtPeriodEnd(Boolean.TRUE.equals(stripeSubscription.getCancelAtPeriodEnd()));

        subscriptionRepository.save(subscription);

        return this.toResponse(subscription);
    }

    @Transactional
    public SubscriptionResponse cancel(UUID id, boolean immediately) {

        Subscription subscription = subscriptionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Subscription not found"));

        com.stripe.model.Subscription stripeSubscription =
                stripeService.cancelSubscription(subscription.getPlan().getCountry(), subscription.getStripeSubscriptionId(), immediately);

        subscription.setStatus(SubscriptionStatus.CANCELED);
        subscription.setCancelAtPeriodEnd(Boolean.TRUE.equals(stripeSubscription.getCancelAtPeriodEnd()));
        subscription.setCurrentPeriodEnd(Instant.ofEpochSecond(stripeSubscription.getStartDate()));

        subscriptionRepository.save(subscription);

        return this.toResponse(subscription);
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
