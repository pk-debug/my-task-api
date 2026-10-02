package com.example.taskapi.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class AuthModels {
    private AuthModels() {
    }

    public record RegisterRequest(
            @NotBlank @Email @Size(max = 254) String email,
            @NotBlank @Size(min = 8, max = 72) String password,
            @NotBlank @Size(max = 80) String name) {
    }

    public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {
    }

    public record RefreshRequest(@NotBlank String refreshToken) {
    }

        public record VerifyEmailRequest(@NotBlank String token) {
        }

        public record ForgotPasswordRequest(@NotBlank @Email String email) {
        }

        public record ResetPasswordRequest(
            @NotBlank String token,
            @NotBlank @Size(min = 8, max = 72) String newPassword) {
        }

        public record MessageResponse(String message) {
        }

    public record UserResponse(Long id, String email, String name) {
        static UserResponse from(UserAccount user) {
            return new UserResponse(user.getId(), user.getEmail(), user.getName());
        }
    }

    public record TokenResponse(
            String tokenType,
            String accessToken,
            String refreshToken,
            long expiresIn,
            UserResponse user) {
    }
}