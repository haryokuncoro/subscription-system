package com.haryokuncoro.subscription_app.service;

import com.haryokuncoro.subscription_app.entity.User;
import com.haryokuncoro.subscription_app.repository.UserRepository;
import com.haryokuncoro.subscription_app.stripe.StripeKeyResolver;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.net.RequestOptions;
import com.stripe.param.CustomerCreateParams;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.UUID;


@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class StripeService {
    @Value("${stripe.skipSignatureCheck}")
    private boolean skipSignatureCheck;
    private final UserRepository userRepository;
    private final StripeKeyResolver stripeKeyResolver;

    public String createCustomer(User user) throws StripeException {
        String apiKey = stripeKeyResolver.resolveApiKey(user.getCountry());
        RequestOptions options = RequestOptions.builder().setApiKey(apiKey).build();

        CustomerCreateParams params = CustomerCreateParams.builder()
                .setEmail(user.getEmail())
                .setName(user.getFullName())
                .build();

        try {
            Customer customer = Customer.create(params, options);
            return customer.getId();
        }catch (StripeException e){
            log.error("fail to create stripe customer", e);
            throw e;
        }
    }

    @Async
    protected void createStripeCustomerAsync(UUID userId) {
        userRepository.findById(userId).ifPresent(user -> {
            try {
                String customerId = this.createCustomer(user);
                user.setStripeCustomerId(customerId);
                userRepository.save(user);
            } catch (Exception e) {
                log.error("Failed to create Stripe customer for user {}", userId, e);
            }
        });
    }
}