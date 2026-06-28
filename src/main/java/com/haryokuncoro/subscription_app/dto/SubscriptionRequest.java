package com.haryokuncoro.subscription_app.dto;

import com.haryokuncoro.subscription_app.dto.enums.SubscriptionStatus;
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

    @NotNull
    private SubscriptionStatus status;

    private Instant currentPeriodStart;

    private Instant currentPeriodEnd;

    private Boolean cancelAtPeriodEnd;
}