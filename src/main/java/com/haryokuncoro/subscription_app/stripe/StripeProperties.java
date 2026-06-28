package com.haryokuncoro.subscription_app.stripe;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "stripe")
public record StripeProperties(
        String apiKeySG,
        String apiKeyUS,
        String apiKeyID,
        boolean mockEnabled,
        String mockBaseUrl
) {}