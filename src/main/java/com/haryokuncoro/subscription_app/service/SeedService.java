package com.haryokuncoro.subscription_app.service;

import com.haryokuncoro.subscription_app.dto.PlanRequest;
import com.haryokuncoro.subscription_app.dto.PlanResponse;
import com.haryokuncoro.subscription_app.dto.SubscriptionRequest;
import com.haryokuncoro.subscription_app.dto.UserRequest;
import com.haryokuncoro.subscription_app.dto.UserResponse;
import com.haryokuncoro.subscription_app.dto.enums.BillingInterval;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor @Slf4j
@Transactional
public class SeedService {
    private final UserService userService;
    private final PlanService planService;
    private final SubscriptionService subscriptionService;

    public void seedData(){
        seedUser();
        seedPlan();
        seedSubscription();
    }


    public void seedUser(){
       List<UserRequest> userRequests = List.of(
               UserRequest.builder()
                       .email("admin@mail.com")
                       .fullName("Admin")
                       .password("password")
                       .country("singapore")
                       .build(),
               UserRequest.builder()
                       .email("admin2@mail.com")
                       .fullName("Admin Two")
                       .password("password")
                       .country("singapore")
                       .build(),
               UserRequest.builder()
                       .email("admin3@mail.com")
                       .fullName("Admin Three")
                       .password("password")
                       .country("singapore")
                       .build()

        );

       for(UserRequest request: userRequests){
           userService.create(request);
       }
    }

    public void seedPlan(){
        List<PlanRequest> planRequests = List.of(
                PlanRequest.builder()
                        .name("Starter Monthly")
                        .description("Starter plan billed monthly")
                        .amount(new BigDecimal("9.99"))
                        .currency("SGD")
                        .country("singapore")
                        .billingInterval(BillingInterval.MONTH)
                        .active(true)
                        .build(),
                PlanRequest.builder()
                        .name("Pro Monthly")
                        .description("Professional monthly subscription")
                        .amount(new BigDecimal("29.99"))
                        .currency("SGD")
                        .country("singapore")
                        .billingInterval(BillingInterval.MONTH)
                        .active(true)
                        .build(),
                PlanRequest.builder()
                        .name("Enterprise")
                        .description("Enterprise subscription")
                        .amount(new BigDecimal("999.99"))
                        .currency("SGD")
                        .country("singapore")
                        .billingInterval(BillingInterval.YEAR)
                        .active(true)
                        .build()

        );

        for(PlanRequest request: planRequests){
            planService.create(request);
        }
    }

    public void seedSubscription(){
        List<UserResponse> userResponses = userService.findAll();
        List<PlanResponse> planResponses = planService.findAll();
        PlanResponse plan = planResponses.get(0);
        for(UserResponse user: userResponses){
            subscriptionService.create(SubscriptionRequest.builder()
                            .userId(user.getId())
                            .planId(plan.getId())
                            .currentPeriodStart(Instant.now()).build());
        }
    }


}
