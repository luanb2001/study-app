package com.studyapp.backend.service;

public interface AccountEmailSender {
    void sendVerificationCode(String email, String code);

    void sendPasswordResetCode(String email, String code);
}
