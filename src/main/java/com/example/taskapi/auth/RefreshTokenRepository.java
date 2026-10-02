package com.example.taskapi.auth;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.Instant;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, String> {
	@Modifying
	@Query("update RefreshToken token set token.revoked = true "
			+ "where token.tokenHash = :tokenHash and token.revoked = false and token.expiresAt > :now")
	int revokeIfActive(@Param("tokenHash") String tokenHash, @Param("now") Instant now);

	@Modifying
	@Query("update RefreshToken token set token.revoked = true where token.userId = :userId and token.revoked = false")
	int revokeAllForUser(@Param("userId") Long userId);
}