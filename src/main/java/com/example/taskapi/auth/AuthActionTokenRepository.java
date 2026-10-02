package com.example.taskapi.auth;

import java.time.Instant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AuthActionTokenRepository extends JpaRepository<AuthActionToken, String> {
    @Modifying
    @Query("update AuthActionToken token set token.consumed = true "
            + "where token.userId = :userId and token.purpose = :purpose and token.consumed = false")
    int consumeOutstandingForUser(
            @Param("userId") Long userId,
            @Param("purpose") AuthActionToken.Purpose purpose);

    @Modifying
    @Query("update AuthActionToken token set token.consumed = true "
            + "where token.tokenHash = :tokenHash and token.purpose = :purpose "
            + "and token.consumed = false and token.expiresAt > :now")
    int consumeIfActive(
            @Param("tokenHash") String tokenHash,
            @Param("purpose") AuthActionToken.Purpose purpose,
            @Param("now") Instant now);
}