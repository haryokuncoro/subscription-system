package com.haryokuncoro.subscription_app.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class UserResponse {

    private UUID id;

    private String email;

    private String fullName;

    private String country;

    private String stripeCustomerId;

    private Instant createdAt;

    private Instant updatedAt;

}