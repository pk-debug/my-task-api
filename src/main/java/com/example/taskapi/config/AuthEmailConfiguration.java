package com.example.taskapi.config;

import com.example.taskapi.auth.AuthEmailSender;
import com.example.taskapi.auth.ConsoleAuthEmailSender;
import com.example.taskapi.auth.SmtpAuthEmailSender;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.mail.javamail.JavaMailSender;

@Configuration
public class AuthEmailConfiguration {
    @Bean
    AuthEmailSender authEmailSender(Environment environment, ObjectProvider<JavaMailSender> mailSenderProvider) {
        String host = environment.getProperty("MAIL_HOST", "");
        if (host.isBlank()) {
            return new ConsoleAuthEmailSender();
        }
        String from = environment.getProperty("MAIL_FROM", "no-reply@example.com");
        return new SmtpAuthEmailSender(mailSenderProvider.getObject(), from);
    }
}