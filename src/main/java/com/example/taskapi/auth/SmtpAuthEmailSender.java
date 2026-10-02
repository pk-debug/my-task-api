package com.example.taskapi.auth;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

public class SmtpAuthEmailSender implements AuthEmailSender {
    private final JavaMailSender mailSender;
    private final String from;

    public SmtpAuthEmailSender(JavaMailSender mailSender, String from) {
        this.mailSender = mailSender;
        this.from = from;
    }

    @Override
    public void sendVerification(String email, String token, String endpoint) {
        send(email, "Verify your Task API account", endpoint, token);
    }

    @Override
    public void sendPasswordReset(String email, String token, String endpoint) {
        send(email, "Reset your Task API password", endpoint, token);
    }

    private void send(String email, String subject, String endpoint, String token) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(email);
        message.setSubject(subject);
        message.setText("Use this one-time token with POST " + endpoint + ":\n\n{\"token\":\"" + token + "\"}");
        mailSender.send(message);
    }
}