package com.haryokuncoro.subscription_app.service;

import com.haryokuncoro.subscription_app.dto.AuthResponse;
import com.haryokuncoro.subscription_app.dto.LoginRequest;
import com.haryokuncoro.subscription_app.dto.RegisterRequest;
import com.haryokuncoro.subscription_app.entity.User;
import com.haryokuncoro.subscription_app.exception.BadRequestException;
import com.haryokuncoro.subscription_app.exception.NotFoundException;
import com.haryokuncoro.subscription_app.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private StripeService stripeService;

    @InjectMocks
    private AuthService authService;

    // -------------------------------------------------------
    // register()
    // -------------------------------------------------------

    @Test
    void register_success_returnsAuthResponse() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("test@example.com");
        request.setPassword("password123");
        request.setFullName("Test User");
        request.setCountry("Singapore");

        String token = "jwt.token.here";
        Instant expiry = Instant.now().plusSeconds(3600);

        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashed_password");
        when(jwtService.generateToken(any(User.class))).thenReturn(token);
        when(jwtService.getExpiration(token)).thenReturn(expiry);

        try (MockedStatic<TransactionSynchronizationManager> txMgr =
                     mockStatic(TransactionSynchronizationManager.class)) {
            txMgr.when(() -> TransactionSynchronizationManager.registerSynchronization(any()))
                 .thenAnswer(invocation -> null);

            AuthResponse response = authService.register(request);

            assertThat(response).isNotNull();
            assertThat(response.getAccessToken()).isEqualTo(token);
            assertThat(response.getTokenType()).isEqualTo("Bearer");
            assertThat(response.getExpiresAt()).isEqualTo(expiry);
            verify(userRepository).save(any(User.class));
            verify(jwtService).generateToken(any(User.class));
        }
    }

    @Test
    void register_countryStoredAsLowercase() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("test@example.com");
        request.setPassword("password123");
        request.setFullName("Test User");
        request.setCountry("SINGAPORE");

        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");
        when(jwtService.generateToken(any(User.class))).thenReturn("token");
        when(jwtService.getExpiration(anyString())).thenReturn(Instant.now().plusSeconds(3600));

        try (MockedStatic<TransactionSynchronizationManager> txMgr =
                     mockStatic(TransactionSynchronizationManager.class)) {
            txMgr.when(() -> TransactionSynchronizationManager.registerSynchronization(any()))
                 .thenAnswer(invocation -> null);

            authService.register(request);

            verify(userRepository).save(argThat(user -> "singapore".equals(user.getCountry())));
        }
    }

    @Test
    void register_passwordIsEncoded() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("test@example.com");
        request.setPassword("plaintext123");
        request.setFullName("Test User");
        request.setCountry("singapore");

        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode("plaintext123")).thenReturn("bcrypt_hash");
        when(jwtService.generateToken(any(User.class))).thenReturn("token");
        when(jwtService.getExpiration(anyString())).thenReturn(Instant.now().plusSeconds(3600));

        try (MockedStatic<TransactionSynchronizationManager> txMgr =
                     mockStatic(TransactionSynchronizationManager.class)) {
            txMgr.when(() -> TransactionSynchronizationManager.registerSynchronization(any()))
                 .thenAnswer(invocation -> null);

            authService.register(request);

            verify(userRepository).save(argThat(user -> "bcrypt_hash".equals(user.getPasswordHash())));
        }
    }

    @Test
    void register_emailAlreadyExists_throwsBadRequestException() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("existing@example.com");
        request.setPassword("password123");
        request.setFullName("Test User");
        request.setCountry("Singapore");

        when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Email already exists");

        verify(userRepository, never()).save(any());
        verify(jwtService, never()).generateToken(any());
    }

    @Test
    void register_userIsCreatedAsActive() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("new@example.com");
        request.setPassword("password123");
        request.setFullName("New User");
        request.setCountry("malaysia");

        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");
        when(jwtService.generateToken(any(User.class))).thenReturn("token");
        when(jwtService.getExpiration(anyString())).thenReturn(Instant.now().plusSeconds(3600));

        try (MockedStatic<TransactionSynchronizationManager> txMgr =
                     mockStatic(TransactionSynchronizationManager.class)) {
            txMgr.when(() -> TransactionSynchronizationManager.registerSynchronization(any()))
                 .thenAnswer(invocation -> null);

            authService.register(request);

            verify(userRepository).save(argThat(User::isActive));
        }
    }

    // -------------------------------------------------------
    // login()
    // -------------------------------------------------------

    @Test
    void login_success_returnsAuthResponse() {
        LoginRequest request = new LoginRequest();
        request.setEmail("user@example.com");
        request.setPassword("password123");

        User user = User.builder()
                .email("user@example.com")
                .passwordHash("hashed_password")
                .active(true)
                .country("singapore")
                .build();

        String token = "jwt.token.login";
        Instant expiry = Instant.now().plusSeconds(3600);

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "hashed_password")).thenReturn(true);
        when(jwtService.generateToken(user)).thenReturn(token);
        when(jwtService.getExpiration(token)).thenReturn(expiry);

        AuthResponse response = authService.login(request);

        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo(token);
        assertThat(response.getTokenType()).isEqualTo("Bearer");
        assertThat(response.getExpiresAt()).isEqualTo(expiry);
    }

    @Test
    void login_userNotFound_throwsNotFoundException() {
        LoginRequest request = new LoginRequest();
        request.setEmail("unknown@example.com");
        request.setPassword("password123");

        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("User not found");

        verify(jwtService, never()).generateToken(any());
    }

    @Test
    void login_wrongPassword_throwsBadRequestException() {
        LoginRequest request = new LoginRequest();
        request.setEmail("user@example.com");
        request.setPassword("wrong_password");

        User user = User.builder()
                .email("user@example.com")
                .passwordHash("hashed_password")
                .active(true)
                .country("singapore")
                .build();

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong_password", "hashed_password")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid email or password");

        verify(jwtService, never()).generateToken(any());
    }

    @Test
    void login_inactiveUser_throwsRuntimeException() {
        LoginRequest request = new LoginRequest();
        request.setEmail("inactive@example.com");
        request.setPassword("password123");

        User user = User.builder()
                .email("inactive@example.com")
                .passwordHash("hashed_password")
                .active(false)
                .country("singapore")
                .build();

        when(userRepository.findByEmail("inactive@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "hashed_password")).thenReturn(true);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("user not active");

        verify(jwtService, never()).generateToken(any());
    }
}

