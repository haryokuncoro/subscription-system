package com.haryokuncoro.subscription_app.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
public class SubscriptionRequest {

    @NotNull
    private UUID userId;

    @NotNull
    private UUID planId;

    private Instant currentPeriodStart;

    private Instant currentPeriodEnd;

    private Boolean cancelAtPeriodEnd;
}