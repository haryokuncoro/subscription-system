package com.haryokuncoro.subscription_app.config;

import com.haryokuncoro.subscription_app.stripe.StripeProperties;
import com.stripe.Stripe;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StripeConfig {

    private final StripeProperties properties;

    public StripeConfig(StripeProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    public void init() {
        if (properties.mockEnabled()) {
            Stripe.overrideApiBase(properties.mockBaseUrl());
            Stripe.overrideUploadBase(properties.mockBaseUrl());
        }
    }
}

