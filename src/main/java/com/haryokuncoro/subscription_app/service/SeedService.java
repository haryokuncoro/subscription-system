package com.haryokuncoro.subscription_app.service;

import com.haryokuncoro.subscription_app.dto.PlanRequest;
import com.haryokuncoro.subscription_app.dto.PlanResponse;
import com.haryokuncoro.subscription_app.dto.SubscriptionRequest;
import com.haryokuncoro.subscription_app.dto.SubscriptionResponse;
import com.haryokuncoro.subscription_app.dto.UserRequest;
import com.haryokuncoro.subscription_app.dto.UserResponse;
import com.haryokuncoro.subscription_app.dto.enums.BillingInterval;
import com.haryokuncoro.subscription_app.dto.enums.InvoiceStatus;
import com.haryokuncoro.subscription_app.entity.Invoice;
import com.haryokuncoro.subscription_app.entity.Subscription;
import com.haryokuncoro.subscription_app.repository.InvoiceRepository;
import com.haryokuncoro.subscription_app.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Service
@RequiredArgsConstructor @Slf4j
@Transactional
public class SeedService {
    private final UserService userService;
    private final PlanService planService;
    private final SubscriptionService subscriptionService;
    private final InvoiceRepository invoiceRepository;
    private final SubscriptionRepository subscriptionRepository;

    public void seedData(){
        seedUser();
        seedPlan();
        seedSubscription();
    }

    public void seedUser(){
       List<UserRequest> userRequests = new ArrayList<>();
        String[] firstNames = {"James", "Mary", "John", "Patricia", "Robert", "Linda", "Michael", "Elizabeth"};
        String[] lastNames = {"Smith", "Johnson", "Williams", "Brown", "Jones", "Garcia", "Miller", "Davis"};
        Random random = new Random();

        for(int i=1; i<=100; i++){
            String firstName = firstNames[random.nextInt(firstNames.length)];
            String lastName = lastNames[random.nextInt(lastNames.length)];
            String fullName = firstName + " " + lastName;
            String email = (firstName + "." + lastName + i + "@mail.com").toLowerCase();
            UserRequest req = UserRequest.builder()
                   .email(email)
                   .fullName(fullName)
                   .password("password")
                   .country("singapore")
                   .build();
            userRequests.add(req);
       }

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
            SubscriptionResponse response = subscriptionService.create(SubscriptionRequest.builder()
                            .userId(user.getId())
                            .planId(plan.getId())
                            .currentPeriodStart(Instant.now()).build());
        }
    }

    public void seedInvoice(){
        List<Subscription> subscriptions = subscriptionRepository.findAll();
        List<Invoice> invoices = new ArrayList<>();
        Random random = new Random();
        int i = 1;
        for (Subscription subscription : subscriptions) {
            long subtotal = (random.nextInt(91) + 10) * 100L;
            long tax = Math.round(subtotal * 0.09);
            long total = subtotal + tax;
            Invoice invoice = Invoice.builder()
                    .subscription(subscription)
                    .status(InvoiceStatus.PAID)
                    .subtotal(subtotal)
                    .tax(tax)
                    .total(total)
                    .amountDue(total)
                    .amountPaid(total)
                    .stripeInvoiceId(String.format("inv_%04d", i++))
                    .currency("SGD")
                    .periodStart(Instant.now())
                    .periodEnd(Instant.now().plus(30, ChronoUnit.DAYS))
                    .paidAt(Instant.now())
                    .build();

            invoices.add(invoice);
        }
       invoiceRepository.saveAll(invoices);
    }


}
