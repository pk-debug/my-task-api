package com.example.taskapi.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConsoleAuthEmailSender implements AuthEmailSender {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConsoleAuthEmailSender.class);

    @Override
    public void sendVerification(String email, String token, String endpoint) {
        LOGGER.info("Development verification token for {}: POST {} with token {}", email, endpoint, token);
    }

    @Override
    public void sendPasswordReset(String email, String token, String endpoint) {
        LOGGER.info("Development password-reset token for {}: POST {} with token {}", email, endpoint, token);
    }
}