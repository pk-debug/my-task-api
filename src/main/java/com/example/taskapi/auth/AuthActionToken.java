package com.example.taskapi.auth;

import java.time.Instant;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "auth_action_tokens")
public class AuthActionToken {
    public enum Purpose {
        EMAIL_VERIFICATION,
        PASSWORD_RESET
    }

    @Id
    @Column(length = 64)
    private String tokenHash;

    @Column(nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Purpose purpose;

    @Column(nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private boolean consumed;

    protected AuthActionToken() {
    }

    public AuthActionToken(String tokenHash, Long userId, Purpose purpose, Instant expiresAt) {
        this.tokenHash = tokenHash;
        this.userId = userId;
        this.purpose = purpose;
        this.expiresAt = expiresAt;
    }

    public Long getUserId() {
        return userId;
    }
}