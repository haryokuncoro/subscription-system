package com.haryokuncoro.subscription_app.stripe;

import org.springframework.stereotype.Component;

@Component
public class StripeKeyResolver {
    private final StripeProperties properties;

    public StripeKeyResolver(StripeProperties properties) {
        this.properties = properties;
    }

    public String resolveApiKey(String country) {
        return switch (country.toLowerCase()) {
            case "singapore" -> properties.apiKeySG();
            case "hong kong" -> properties.apiKeyHK();
            case "malaysia" -> properties.apiKeyMY();
            default -> throw new IllegalStateException("Unexpected value: " + country);
        };
    }
}