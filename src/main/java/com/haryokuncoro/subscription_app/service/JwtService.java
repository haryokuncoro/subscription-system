package com.haryokuncoro.subscription_app.service;

import com.haryokuncoro.subscription_app.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Service @Slf4j
@RequiredArgsConstructor
public class JwtService {
    @Value("${jwt.expiration}")
    private long expiration;
    @Value("${jwt.secret}")
    private String secret;


    public String generateToken(User user) {
        Instant now = Instant.now();
        Instant expires = now.plusMillis(expiration);
        return Jwts.builder()
                .setSubject(user.getId().toString())
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(expires))
                .signWith(getKey())
                .compact();
    }

    public UUID getUserId(String token) {
        return UUID.fromString(parse(token).getSubject());
    }

    public boolean isValid(String token, User user) {
        try {
            UUID userId = getUserId(token);
            return userId.equals(user.getId()) && !isExpired(token);
        } catch (Exception e) {
            log.error("Invalid token: {}", e.getMessage());
            return false;
        }
    }

    public Instant getExpiration(String token) {
        return parse(token).getExpiration().toInstant();
    }

    private boolean isExpired(String token) {
        return getExpiration(token).isBefore(Instant.now());
    }

    private SecretKey getKey() {
        return Keys.hmacShaKeyFor(
                secret.getBytes(StandardCharsets.UTF_8)
        );
    }

    private Claims parse(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

}