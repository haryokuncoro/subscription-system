package com.haryokuncoro.subscription_app.dto;

import com.haryokuncoro.subscription_app.dto.enums.BillingInterval;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class PlanResponse {

    private UUID id;

    private String name;

    private String description;

    private String stripeProductId;

    private String stripePriceId;

    private BigDecimal amount;

    private String currency;
    private String country;

    private BillingInterval billingInterval;

    private boolean active;

    private Instant createdAt;

    private Instant updatedAt;

}