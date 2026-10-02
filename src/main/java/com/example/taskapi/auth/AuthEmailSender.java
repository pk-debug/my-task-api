package com.example.taskapi.auth;

public interface AuthEmailSender {
    void sendVerification(String email, String token, String endpoint);

    void sendPasswordReset(String email, String token, String endpoint);
}