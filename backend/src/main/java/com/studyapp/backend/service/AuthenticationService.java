package com.studyapp.backend.service;

import com.studyapp.backend.security.AccountPrincipal;
import com.studyapp.backend.security.JwtTokenService;
import com.studyapp.backend.repository.AuthenticationRepository;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Locale;
import java.util.UUID;

@Service
public class AuthenticationService {
    private static final String VERIFY_EMAIL = "VERIFY_EMAIL";
    private static final String RESET_PASSWORD = "RESET_PASSWORD";
    private static final int MAX_CODE_ATTEMPTS = 5;
    private static final long ACTION_CODE_LIFETIME_SECONDS = 15 * 60;
    private static final long ACTION_CODE_COOLDOWN_SECONDS = 60;
    private static final long REFRESH_TOKEN_LIFETIME_SECONDS = 30L * 24 * 60 * 60;

    private final AuthenticationRepository authenticationRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;
    private final AccountEmailSender accountEmailSender;
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthenticationService(
            AuthenticationRepository authenticationRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenService jwtTokenService,
            AccountEmailSender accountEmailSender) {
        this.authenticationRepository = authenticationRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenService = jwtTokenService;
        this.accountEmailSender = accountEmailSender;
    }

    @Transactional
    public MessageResponse register(String email, String password) {
        String normalizedEmail = this.normalizeEmail(email);
        this.validatePassword(password);
        String userId = UUID.randomUUID().toString();

        try {
            this.authenticationRepository.insertUser(userId, normalizedEmail, this.passwordEncoder.encode(password));
        } catch (DuplicateKeyException exception) {
            return new MessageResponse("If the address can be registered, a verification code will be sent.");
        }

        this.issueActionCode(userId, normalizedEmail, VERIFY_EMAIL);
        return new MessageResponse("If the address can be registered, a verification code will be sent.");
    }

    @Transactional
    public AuthenticationResponse login(String email, String password) {
        String normalizedEmail = this.normalizeEmail(email);

        if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new InvalidCredentialsException();
        }

        AuthenticationRepository.UserAccount user = this.authenticationRepository.userByEmail(normalizedEmail)
                .orElseThrow(InvalidCredentialsException::new);

        if (!this.passwordEncoder.matches(password, user.passwordHash())) {
            throw new InvalidCredentialsException();
        }

        if (!user.emailVerified()) {
            throw new EmailNotVerifiedException();
        }

        return this.createSession(new AccountPrincipal(user.id(), user.email()));
    }

    @Transactional(noRollbackFor = InvalidActionCodeException.class)
    public AuthenticationResponse verifyEmail(String email, String code) {
        String userId = this.userIdForEmail(email);
        this.verifyActionCode(userId, VERIFY_EMAIL, code);
        this.authenticationRepository.markEmailVerified(userId);
        this.authenticationRepository.deleteActionCodesForUserAndPurpose(userId, VERIFY_EMAIL);
        AccountPrincipal principal = new AccountPrincipal(userId, this.normalizeEmail(email));
        return this.createSession(principal);
    }

    @Transactional
    public MessageResponse resendVerification(String email) {
        this.authenticationRepository.usersByEmailAndVerification(this.normalizeEmail(email), false)
                .forEach(user -> this.issueActionCode(user.id(), user.email(), VERIFY_EMAIL));
        return new MessageResponse("If the account requires verification, a code will be sent.");
    }

    @Transactional
    public MessageResponse requestPasswordReset(String email) {
        this.authenticationRepository.usersByEmailAndVerification(this.normalizeEmail(email), true)
                .forEach(user -> this.issueActionCode(user.id(), user.email(), RESET_PASSWORD));
        return new MessageResponse("If a verified account exists, a reset code will be sent.");
    }

    @Transactional(noRollbackFor = InvalidActionCodeException.class)
    public MessageResponse resetPassword(String email, String code, String newPassword) {
        this.validatePassword(newPassword);
        String userId = this.userIdForEmail(email);
        this.verifyActionCode(userId, RESET_PASSWORD, code);
        this.authenticationRepository.updatePassword(userId, this.passwordEncoder.encode(newPassword));
        this.authenticationRepository.revokeRefreshTokensForUser(userId);
        this.authenticationRepository.deleteActionCodesForUser(userId);
        return new MessageResponse("Password updated. Sign in again with the new password.");
    }

    @Transactional
    public AuthenticationResponse refresh(String refreshToken) {

        if (refreshToken == null || refreshToken.isBlank()) {
            throw new InvalidRefreshTokenException();
        }

        String hash = this.sha256(refreshToken);
        AuthenticationRepository.RefreshAccount account = this.authenticationRepository.refreshAccountForUpdate(hash)
                .orElseThrow(InvalidRefreshTokenException::new);
        if (account.revoked() || !account.expiresAt().isAfter(Instant.now())
                || !account.emailVerified()) {
                throw new InvalidRefreshTokenException();
        }
        this.authenticationRepository.revokeRefreshToken(hash);
        return this.createSession(new AccountPrincipal(account.userId(), account.email()));
    }

    @Transactional
    public MessageResponse logout(String refreshToken) {

        if (refreshToken != null && !refreshToken.isBlank()) {
            this.authenticationRepository.revokeRefreshToken(this.sha256(refreshToken));
        }

        return new MessageResponse("Signed out.");
    }

    private AuthenticationResponse createSession(AccountPrincipal principal) {
        this.authenticationRepository.deleteExpiredRefreshTokens(Instant.now());
        String refreshToken = this.randomToken(32);
        this.authenticationRepository.insertRefreshToken(this.sha256(refreshToken), principal.id(),
                Instant.now().plusSeconds(REFRESH_TOKEN_LIFETIME_SECONDS));
        return new AuthenticationResponse(
                principal.id(), principal.email(), this.jwtTokenService.createToken(principal), refreshToken);
    }

    private void issueActionCode(String userId, String email, String purpose) {
        Instant now = Instant.now();
        this.authenticationRepository.deleteExpiredActionCodes(now);
        if (this.authenticationRepository.hasRecentlyIssuedActionCode(userId, purpose,
                now.minusSeconds(ACTION_CODE_COOLDOWN_SECONDS))) return;

        this.authenticationRepository.deleteActionCodesForUserAndPurpose(userId, purpose);
        String code = String.format(Locale.ROOT, "%06d", this.secureRandom.nextInt(1_000_000));
        this.authenticationRepository.insertActionCode(UUID.randomUUID().toString(), userId, purpose,
                this.passwordEncoder.encode(code), now.plusSeconds(ACTION_CODE_LIFETIME_SECONDS));

        if (VERIFY_EMAIL.equals(purpose)) {
            this.accountEmailSender.sendVerificationCode(email, code);
        } else {
            this.accountEmailSender.sendPasswordResetCode(email, code);
        }

    }

    private void verifyActionCode(String userId, String purpose, String code) {
        AuthenticationRepository.ActionCode stored = this.authenticationRepository.actionCodeForUpdate(userId, purpose)
                .orElseThrow(InvalidActionCodeException::new);

        if (!stored.expiresAt().isAfter(Instant.now()) || stored.attempts() >= MAX_CODE_ATTEMPTS) {
            this.authenticationRepository.deleteActionCode(stored.id());
            throw new InvalidActionCodeException();
        }

        if (!this.passwordEncoder.matches(code, stored.tokenHash())) {
            int attempts = stored.attempts() + 1;

            if (attempts >= MAX_CODE_ATTEMPTS) {
                this.authenticationRepository.deleteActionCode(stored.id());
            } else {
                this.authenticationRepository.updateActionCodeAttempts(stored.id(), attempts);
            }

            throw new InvalidActionCodeException();
        }

    }

    private String userIdForEmail(String email) {
        return this.authenticationRepository.userIdByEmail(this.normalizeEmail(email))
                .orElseThrow(InvalidActionCodeException::new);
    }

    private void validatePassword(String password) {

        if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalArgumentException("Password must not exceed 72 UTF-8 bytes.");
        }

    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String randomToken(int byteCount) {
        byte[] bytes = new byte[byteCount];
        this.secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String sha256(String token) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(hash);
        } catch (java.security.NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable.", exception);
        }
    }

    public record AuthenticationResponse(
            String accountId, String email, String accessToken, String refreshToken) {}

    public record MessageResponse(String message) {}

    public static class InvalidCredentialsException extends RuntimeException {}

    public static class EmailNotVerifiedException extends RuntimeException {}

    public static class InvalidRefreshTokenException extends RuntimeException {}

    public static class InvalidActionCodeException extends RuntimeException {}
}
