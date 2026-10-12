package com.studyapp.backend.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

@Component
public class SmtpAccountEmailSender implements AccountEmailSender {
    private final JavaMailSender javaMailSender;
    private final String fromAddress;

    public SmtpAccountEmailSender(
            JavaMailSender javaMailSender,
            @Value("${studyapp.mail.from}") String fromAddress) {
        this.javaMailSender = javaMailSender;
        this.fromAddress = fromAddress;
    }

    @Override
    public void sendVerificationCode(String email, String code) {
        this.send(email, "Verify your Study App email",
                "Your Study App verification code is " + code
                        + ". It expires in 15 minutes. If you did not create this account, ignore this email.");
    }

    @Override
    public void sendPasswordResetCode(String email, String code) {
        this.send(email, "Reset your Study App password",
                "Your Study App password reset code is " + code
                        + ". It expires in 15 minutes. If you did not request this change, ignore this email.");
    }

    private void send(String email, String subject, String body) {
        MimeMessage message = this.javaMailSender.createMimeMessage();

        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
            helper.setFrom(this.fromAddress);
            helper.setTo(email);
            helper.setSubject(subject);
            helper.setText(body, false);
        } catch (MessagingException exception) {
            throw new IllegalStateException("Could not prepare account email.", exception);
        }

        this.javaMailSender.send(message);
    }
}
