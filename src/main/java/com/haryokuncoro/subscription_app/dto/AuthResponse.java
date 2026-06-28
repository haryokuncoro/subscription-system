package com.haryokuncoro.subscription_app.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Builder
@Getter
public class AuthResponse {

    private String accessToken;

    private String tokenType;

    private Instant expiresAt;

}