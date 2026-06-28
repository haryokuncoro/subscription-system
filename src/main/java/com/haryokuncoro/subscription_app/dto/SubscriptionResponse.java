package com.haryokuncoro.subscription_app.dto;

import com.haryokuncoro.subscription_app.dto.enums.SubscriptionStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class SubscriptionResponse {

    private UUID id;

    private UUID userId;

    private String userEmail;

    private UUID planId;

    private String planName;

    private String stripeSubscriptionId;

    private SubscriptionStatus status;

    private Instant currentPeriodStart;

    private Instant currentPeriodEnd;

    private boolean cancelAtPeriodEnd;

    private Instant canceledAt;

    private Instant createdAt;

    private Instant updatedAt;
}
