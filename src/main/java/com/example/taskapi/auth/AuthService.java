package com.example.taskapi.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserAccountRepository users;
    private final RefreshTokenRepository refreshTokens;
    private final AuthActionTokenRepository actionTokens;
    private final AuthEmailSender emailSender;
    private final PasswordEncoder passwordEncoder;
    private final JwtEncoder jwtEncoder;
    private final Duration accessTtl;
    private final Duration refreshTtl;
    private final String issuer;
    private final String publicBaseUrl;

    public AuthService(
            UserAccountRepository users,
            RefreshTokenRepository refreshTokens,
            AuthActionTokenRepository actionTokens,
            AuthEmailSender emailSender,
            PasswordEncoder passwordEncoder,
            JwtEncoder jwtEncoder,
            @Value("${app.jwt.issuer:task-api}") String issuer,
            @Value("${app.jwt.access-token-ttl:PT15M}") Duration accessTtl,
            @Value("${app.jwt.refresh-token-ttl:P7D}") Duration refreshTtl,
            @Value("${app.public-base-url:http://localhost:8080}") String publicBaseUrl) {
        this.users = users;
        this.refreshTokens = refreshTokens;
        this.actionTokens = actionTokens;
        this.emailSender = emailSender;
        this.passwordEncoder = passwordEncoder;
        this.jwtEncoder = jwtEncoder;
        this.issuer = issuer;
        this.accessTtl = accessTtl;
        this.refreshTtl = refreshTtl;
        this.publicBaseUrl = publicBaseUrl.replaceAll("/+$", "");
    }

    @Transactional
    public AuthModels.MessageResponse register(AuthModels.RegisterRequest request) {
        String email = normalizeEmail(request.email());
        if (request.password().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password must not exceed 72 UTF-8 bytes");
        }
        if (users.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An account with this email already exists");
        }
        UserAccount user = users.save(new UserAccount(email, request.name().trim(), passwordEncoder.encode(request.password())));
        String token = createActionToken(user, AuthActionToken.Purpose.EMAIL_VERIFICATION, Duration.ofHours(24));
        emailSender.sendVerification(email, token, publicBaseUrl + "/auth/verify-email");
        return new AuthModels.MessageResponse("Check your email for a verification token");
    }

    @Transactional
    public AuthModels.TokenResponse login(AuthModels.LoginRequest request) {
        UserAccount user = users.findByEmail(normalizeEmail(request.email()))
                .filter(UserAccount::isEmailVerified)
                .filter(account -> passwordEncoder.matches(request.password(), account.getPasswordHash()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));
        return issueTokens(user);
    }

    @Transactional
    public AuthModels.TokenResponse verifyEmail(String rawToken) {
        Long userId = consumeActionToken(rawToken, AuthActionToken.Purpose.EMAIL_VERIFICATION);
        UserAccount user = users.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid verification token"));
        user.verifyEmail();
        return issueTokens(user);
    }

    @Transactional
    public AuthModels.MessageResponse forgotPassword(String emailAddress) {
        users.findByEmail(normalizeEmail(emailAddress))
                .filter(UserAccount::isEmailVerified)
                .ifPresent(user -> {
                    String token = createActionToken(user, AuthActionToken.Purpose.PASSWORD_RESET, Duration.ofHours(1));
                    emailSender.sendPasswordReset(user.getEmail(), token, publicBaseUrl + "/auth/reset-password");
                });
        return new AuthModels.MessageResponse("If the account is verified, a password reset message has been sent");
    }

    @Transactional
    public AuthModels.MessageResponse resendVerification(String emailAddress) {
        users.findByEmail(normalizeEmail(emailAddress))
                .filter(user -> !user.isEmailVerified())
                .ifPresent(user -> {
                    String token = createActionToken(user, AuthActionToken.Purpose.EMAIL_VERIFICATION, Duration.ofHours(24));
                    emailSender.sendVerification(user.getEmail(), token, publicBaseUrl + "/auth/verify-email");
                });
        return new AuthModels.MessageResponse("If the account needs verification, a verification message has been sent");
    }

    @Transactional
    public AuthModels.MessageResponse resetPassword(AuthModels.ResetPasswordRequest request) {
        if (request.newPassword().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password must not exceed 72 UTF-8 bytes");
        }
        Long userId = consumeActionToken(request.token(), AuthActionToken.Purpose.PASSWORD_RESET);
        UserAccount user = users.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid password reset token"));
        user.changePassword(passwordEncoder.encode(request.newPassword()));
        refreshTokens.revokeAllForUser(userId);
        return new AuthModels.MessageResponse("Password updated; log in again with the new password");
    }

    @Transactional
    public AuthModels.TokenResponse refresh(String rawToken) {
        String tokenHash = hash(rawToken);
        if (refreshTokens.revokeIfActive(tokenHash, Instant.now()) != 1) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token");
        }
        RefreshToken token = refreshTokens.findById(tokenHash)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token"));
        UserAccount user = users.findById(token.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token"));
        return issueTokens(user);
    }

    @Transactional
    public void logout(String rawToken) {
        refreshTokens.findById(hash(rawToken)).ifPresent(token -> token.revoke());
    }

    @Transactional(readOnly = true)
    public AuthModels.UserResponse profile(Long userId) {
        return users.findById(userId)
                .map(AuthModels.UserResponse::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Account no longer exists"));
    }

    private AuthModels.TokenResponse issueTokens(UserAccount user) {
        Instant now = Instant.now();
        Instant accessExpiresAt = now.plus(accessTtl);
        String accessToken = jwtEncoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(),
                JwtClaimsSet.builder()
                        .issuer(issuer)
                        .issuedAt(now)
                        .expiresAt(accessExpiresAt)
                        .subject(user.getId().toString())
                        .id(UUID.randomUUID().toString())
                        .claim("email", user.getEmail())
                        .claim("name", user.getName())
                        .build())).getTokenValue();

        byte[] randomBytes = new byte[32];
        SECURE_RANDOM.nextBytes(randomBytes);
        String refreshToken = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
        refreshTokens.save(new RefreshToken(hash(refreshToken), user.getId(), now.plus(refreshTtl)));
        return new AuthModels.TokenResponse("Bearer", accessToken, refreshToken, accessTtl.toSeconds(),
                AuthModels.UserResponse.from(user));
    }

    private String createActionToken(UserAccount user, AuthActionToken.Purpose purpose, Duration ttl) {
        byte[] randomBytes = new byte[32];
        SECURE_RANDOM.nextBytes(randomBytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
        actionTokens.consumeOutstandingForUser(user.getId(), purpose);
        actionTokens.save(new AuthActionToken(hash(rawToken), user.getId(), purpose, Instant.now().plus(ttl)));
        return rawToken;
    }

    private Long consumeActionToken(String rawToken, AuthActionToken.Purpose purpose) {
        String tokenHash = hash(rawToken);
        if (actionTokens.consumeIfActive(tokenHash, purpose, Instant.now()) != 1) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired token");
        }
        return actionTokens.findById(tokenHash)
                .map(AuthActionToken::getUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired token"));
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase(java.util.Locale.ROOT);
    }

    private static String hash(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}