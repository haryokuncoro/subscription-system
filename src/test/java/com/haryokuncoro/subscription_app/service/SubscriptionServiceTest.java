package com.haryokuncoro.subscription_app.service;

import com.haryokuncoro.subscription_app.dto.SubscriptionRequest;
import com.haryokuncoro.subscription_app.dto.SubscriptionResponse;
import com.haryokuncoro.subscription_app.dto.enums.SubscriptionStatus;
import com.haryokuncoro.subscription_app.entity.Plan;
import com.haryokuncoro.subscription_app.entity.Subscription;
import com.haryokuncoro.subscription_app.entity.User;
import com.haryokuncoro.subscription_app.exception.NotFoundException;
import com.haryokuncoro.subscription_app.exception.StripeOperationException;
import com.haryokuncoro.subscription_app.repository.PlanRepository;
import com.haryokuncoro.subscription_app.repository.SubscriptionRepository;
import com.haryokuncoro.subscription_app.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubscriptionServiceTest {

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PlanRepository planRepository;

    @Mock
    private StripeService stripeService;

    @InjectMocks
    private SubscriptionService subscriptionService;

    @Test
    void search_withFilters_mapsRepositoryResultToResponse() {
        UUID userId = UUID.randomUUID();
        UUID planId = UUID.randomUUID();
        UUID subscriptionId = UUID.randomUUID();

        User user = User.builder().email("user@example.com").build();
        user.setId(userId);

        Plan plan = Plan.builder().name("Pro").build();
        plan.setId(planId);

        Subscription subscription = Subscription.builder()
                .user(user)
                .plan(plan)
                .stripeSubscriptionId("sub_123")
                .status(SubscriptionStatus.ACTIVE)
                .currentPeriodStart(Instant.parse("2026-01-01T00:00:00Z"))
                .currentPeriodEnd(Instant.parse("2026-02-01T00:00:00Z"))
                .cancelAtPeriodEnd(false)
                .build();
        subscription.setId(subscriptionId);

        Pageable pageable = PageRequest.of(0, 10);
        Page<Subscription> page = new PageImpl<>(List.of(subscription), pageable, 1);

        when(subscriptionRepository.findAll(org.mockito.ArgumentMatchers.<Specification<Subscription>>any(), eq(pageable)))
                .thenReturn(page);

        Page<SubscriptionResponse> result = subscriptionService.search(userId, planId, "active", pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getStripeSubscriptionId()).isEqualTo("sub_123");
        assertThat(result.getContent().get(0).getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        verify(subscriptionRepository)
                .findAll(org.mockito.ArgumentMatchers.<Specification<Subscription>>any(), eq(pageable));
    }

    @Test
    void findById_notFound_throwsNotFoundException() {
        UUID id = UUID.randomUUID();
        when(subscriptionRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> subscriptionService.findById(id))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Subscription not found");
    }

    @Test
    void create_userNotFound_throwsNotFoundException() {
        SubscriptionRequest request = SubscriptionRequest.builder()
                .userId(UUID.randomUUID())
                .planId(UUID.randomUUID())
                .build();

        when(userRepository.findById(request.getUserId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> subscriptionService.create(request))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("User not found");

        verify(planRepository, never()).findById(any());
        verify(stripeService, never()).createSubscription(any(), any(), any());
    }

    @Test
    void create_whenUserHasNoStripeCustomerId_createsCustomerAndSubscription() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID planId = UUID.randomUUID();

        User user = User.builder()
                .email("user@example.com")
                .fullName("Test User")
                .country("singapore")
                .stripeCustomerId(null)
                .build();
        user.setId(userId);

        Plan plan = Plan.builder()
                .name("Starter")
                .country("singapore")
                .stripePriceId("price_123")
                .build();
        plan.setId(planId);

        SubscriptionRequest request = SubscriptionRequest.builder()
                .userId(userId)
                .planId(planId)
                .build();

        com.stripe.model.Subscription stripeSubscription = mock(com.stripe.model.Subscription.class);
        when(stripeSubscription.getId()).thenReturn("sub_123");
        when(stripeSubscription.getStatus()).thenReturn("active");
        when(stripeSubscription.getStartDate()).thenReturn(1710000000L);
        when(stripeSubscription.getEndedAt()).thenReturn(1712600000L);
        when(stripeSubscription.getCancelAtPeriodEnd()).thenReturn(false);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(planRepository.findById(planId)).thenReturn(Optional.of(plan));
        when(stripeService.createCustomer(user)).thenReturn("cus_123");
        when(stripeService.createSubscription("cus_123", "singapore", "price_123"))
                .thenReturn(stripeSubscription);

        SubscriptionResponse response = subscriptionService.create(request);

        assertThat(response.getStripeSubscriptionId()).isEqualTo("sub_123");
        assertThat(response.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(user.getStripeCustomerId()).isEqualTo("cus_123");
        verify(userRepository).save(user);
        verify(subscriptionRepository).save(any(Subscription.class));
    }

    @Test
    void create_whenCreateCustomerFails_throwsStripeOperationException() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID planId = UUID.randomUUID();

        User user = User.builder()
                .country("singapore")
                .stripeCustomerId(null)
                .build();
        user.setId(userId);

        Plan plan = Plan.builder()
                .country("singapore")
                .stripePriceId("price_123")
                .build();
        plan.setId(planId);

        SubscriptionRequest request = SubscriptionRequest.builder()
                .userId(userId)
                .planId(planId)
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(planRepository.findById(planId)).thenReturn(Optional.of(plan));
        when(stripeService.createCustomer(user)).thenThrow(new RuntimeException("stripe down"));

        assertThatThrownBy(() -> subscriptionService.create(request))
                .isInstanceOf(StripeOperationException.class)
                .hasMessageContaining("fail to create customer while creating subscription");

        verify(subscriptionRepository, never()).save(any(Subscription.class));
    }

    @Test
    void update_planChanged_callsStripeWithNewPriceAndPersistsChanges() {
        UUID subscriptionId = UUID.randomUUID();
        UUID newPlanId = UUID.randomUUID();

        User user = User.builder().email("user@example.com").build();
        user.setId(UUID.randomUUID());

        Plan oldPlan = Plan.builder()
                .name("Old")
                .country("singapore")
                .stripePriceId("price_old")
                .build();
        oldPlan.setId(UUID.randomUUID());

        Plan newPlan = Plan.builder()
                .name("New")
                .country("singapore")
                .stripePriceId("price_new")
                .build();
        newPlan.setId(newPlanId);

        Subscription subscription = Subscription.builder()
                .user(user)
                .plan(oldPlan)
                .stripeSubscriptionId("sub_123")
                .status(SubscriptionStatus.ACTIVE)
                .build();
        subscription.setId(subscriptionId);

        SubscriptionRequest request = SubscriptionRequest.builder()
                .planId(newPlanId)
                .cancelAtPeriodEnd(true)
                .build();

        com.stripe.model.Subscription stripeSubscription = mock(com.stripe.model.Subscription.class);
        when(stripeSubscription.getStatus()).thenReturn("past_due");
        when(stripeSubscription.getStartDate()).thenReturn(1710000100L);
        when(stripeSubscription.getEndedAt()).thenReturn(1712600100L);
        when(stripeSubscription.getCancelAtPeriodEnd()).thenReturn(true);

        when(subscriptionRepository.findById(subscriptionId)).thenReturn(Optional.of(subscription));
        when(planRepository.findById(newPlanId)).thenReturn(Optional.of(newPlan));
        when(stripeService.updateSubscription("singapore", "sub_123", "price_new", true))
                .thenReturn(stripeSubscription);

        SubscriptionResponse response = subscriptionService.update(subscriptionId, request);

        assertThat(response.getPlanId()).isEqualTo(newPlanId);
        assertThat(response.getStatus()).isEqualTo(SubscriptionStatus.PAST_DUE);
        assertThat(response.isCancelAtPeriodEnd()).isTrue();
        verify(subscriptionRepository).save(subscription);
    }

    @Test
    void cancel_marksSubscriptionCanceled_andSaves() {
        UUID subscriptionId = UUID.randomUUID();

        Plan plan = Plan.builder().country("singapore").build();
        User user = User.builder().email("user@example.com").build();

        Subscription subscription = Subscription.builder()
                .plan(plan)
                .user(user)
                .stripeSubscriptionId("sub_123")
                .status(SubscriptionStatus.ACTIVE)
                .cancelAtPeriodEnd(false)
                .build();
        subscription.setId(subscriptionId);

        com.stripe.model.Subscription stripeSubscription = mock(com.stripe.model.Subscription.class);
        when(stripeSubscription.getCancelAtPeriodEnd()).thenReturn(true);
        when(stripeSubscription.getStartDate()).thenReturn(1710000200L);

        when(subscriptionRepository.findById(subscriptionId)).thenReturn(Optional.of(subscription));
        when(stripeService.cancelSubscription("singapore", "sub_123", true)).thenReturn(stripeSubscription);

        SubscriptionResponse response = subscriptionService.cancel(subscriptionId, true);

        assertThat(response.getStatus()).isEqualTo(SubscriptionStatus.CANCELED);
        assertThat(response.isCancelAtPeriodEnd()).isTrue();
        assertThat(response.getCurrentPeriodEnd()).isEqualTo(Instant.ofEpochSecond(1710000200L));
        verify(subscriptionRepository).save(subscription);
    }

    @Test
    void toResponse_mapsAllExpectedFields() {
        UUID userId = UUID.randomUUID();
        UUID planId = UUID.randomUUID();
        UUID subscriptionId = UUID.randomUUID();

        User user = User.builder().email("user@example.com").build();
        user.setId(userId);

        Plan plan = Plan.builder().name("Starter").build();
        plan.setId(planId);

        Instant now = Instant.parse("2026-01-01T00:00:00Z");

        Subscription subscription = Subscription.builder()
                .user(user)
                .plan(plan)
                .stripeSubscriptionId("sub_123")
                .status(SubscriptionStatus.TRIALING)
                .currentPeriodStart(now)
                .currentPeriodEnd(now.plusSeconds(3600))
                .cancelAtPeriodEnd(true)
                .canceledAt(now.plusSeconds(120))
                .build();
        subscription.setId(subscriptionId);
        subscription.setCreatedAt(now.minusSeconds(60));
        subscription.setUpdatedAt(now.plusSeconds(60));

        SubscriptionResponse response = subscriptionService.toResponse(subscription);

        assertThat(response.getId()).isEqualTo(subscriptionId);
        assertThat(response.getUserId()).isEqualTo(userId);
        assertThat(response.getUserEmail()).isEqualTo("user@example.com");
        assertThat(response.getPlanId()).isEqualTo(planId);
        assertThat(response.getPlanName()).isEqualTo("Starter");
        assertThat(response.getStatus()).isEqualTo(SubscriptionStatus.TRIALING);
        assertThat(response.getCreatedAt()).isEqualTo(now.minusSeconds(60));
        assertThat(response.getUpdatedAt()).isEqualTo(now.plusSeconds(60));
    }
}


