package com.haryokuncoro.subscription_app.service;

import com.haryokuncoro.subscription_app.dto.AuthResponse;
import com.haryokuncoro.subscription_app.dto.LoginRequest;
import com.haryokuncoro.subscription_app.dto.RegisterRequest;
import com.haryokuncoro.subscription_app.entity.User;
import com.haryokuncoro.subscription_app.exception.BadRequestException;
import com.haryokuncoro.subscription_app.exception.NotFoundException;
import com.haryokuncoro.subscription_app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
@RequiredArgsConstructor @Slf4j
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final StripeService stripeService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email already exists");
        }

        User user = User.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .country(request.getCountry().toLowerCase())
                .build();

        userRepository.save(user);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                stripeService.createStripeCustomerAsync(user.getId());
            }
        });

        String token = jwtService.generateToken(user);
        return AuthResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .expiresAt(jwtService.getExpiration(token))
                .build();
    }

    public AuthResponse login(LoginRequest request) {

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new NotFoundException("User not found"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BadRequestException("Invalid email or password");
        }

        String token = jwtService.generateToken(user);

        return AuthResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .expiresAt(jwtService.getExpiration(token))
                .build();
    }
}
