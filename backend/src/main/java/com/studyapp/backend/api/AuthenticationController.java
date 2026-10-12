package com.studyapp.backend.api;

import com.studyapp.backend.service.AuthenticationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthenticationController {

    private final AuthenticationService authenticationService;

    public AuthenticationController(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthenticationService.MessageResponse register(
            @Valid @RequestBody AuthenticationRequest request) {
        return this.authenticationService.register(request.email(), request.password());
    }

    @PostMapping("/login")
    public AuthenticationService.AuthenticationResponse login(
            @Valid @RequestBody AuthenticationRequest request) {
        return this.authenticationService.login(request.email(), request.password());
    }

    @PostMapping("/verify-email")
    public AuthenticationService.AuthenticationResponse verifyEmail(
            @Valid @RequestBody CodeRequest request) {
        return this.authenticationService.verifyEmail(request.email(), request.code());
    }

    @PostMapping("/resend-verification")
    public AuthenticationService.MessageResponse resendVerification(
            @Valid @RequestBody EmailRequest request) {
        return this.authenticationService.resendVerification(request.email());
    }

    @PostMapping("/password/forgot")
    public AuthenticationService.MessageResponse requestPasswordReset(
            @Valid @RequestBody EmailRequest request) {
        return this.authenticationService.requestPasswordReset(request.email());
    }

    @PostMapping("/password/reset")
    public AuthenticationService.MessageResponse resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {
        return this.authenticationService.resetPassword(request.email(), request.code(), request.newPassword());
    }

    @PostMapping("/refresh")
    public AuthenticationService.AuthenticationResponse refresh(
            @Valid @RequestBody RefreshRequest request) {
        return this.authenticationService.refresh(request.refreshToken());
    }

    @PostMapping("/logout")
    public AuthenticationService.MessageResponse logout(
            @Valid @RequestBody RefreshRequest request) {
        return this.authenticationService.logout(request.refreshToken());
    }

    public record EmailRequest(@NotBlank @Email @Size(max = 254) String email) {}

    public record CodeRequest(
            @NotBlank @Email @Size(max = 254) String email,
            @NotBlank @Pattern(regexp = "\\d{6}") String code) {}

    public record ResetPasswordRequest(
            @NotBlank @Email @Size(max = 254) String email,
            @NotBlank @Pattern(regexp = "\\d{6}") String code,
            @NotBlank @Size(min = 8, max = 72) String newPassword) {}

    public record RefreshRequest(@NotBlank String refreshToken) {}

    public record AuthenticationRequest(
            @NotBlank @Email @Size(max = 254) String email,
            @NotBlank @Size(min = 8, max = 72) String password) {
    }
}
