package com.studyapp.backend.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public class AuthenticationRepository {
    private final JdbcTemplate jdbcTemplate;

    public AuthenticationRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void insertUser(String id, String email, String passwordHash) {
        this.jdbcTemplate.update("""
                INSERT INTO app_users (id, email, password_hash, email_verified)
                VALUES (?, ?, ?, FALSE)
                """, id, email, passwordHash);
    }

    public Optional<UserAccount> userByEmail(String email) {
        return this.jdbcTemplate.query("""
                SELECT id, email, password_hash, email_verified
                FROM app_users WHERE email = ?
                """, (rs, row) -> new UserAccount(
                rs.getString("id"), rs.getString("email"), rs.getString("password_hash"),
                rs.getBoolean("email_verified")), email).stream().findFirst();

    }

    public Optional<String> userIdByEmail(String email) {

        return this.jdbcTemplate.query("SELECT id FROM app_users WHERE email = ?",
                (rs, row) -> rs.getString("id"), email).stream().findFirst();

    }

    public List<UserEmail> usersByEmailAndVerification(String email, boolean verified) {
        return this.jdbcTemplate.query("""
                SELECT id, email FROM app_users WHERE email = ? AND email_verified = ?
                """, (rs, row) -> new UserEmail(rs.getString("id"), rs.getString("email")),
                email, verified);
    }

    public void markEmailVerified(String userId) {
        this.jdbcTemplate.update("UPDATE app_users SET email_verified = TRUE WHERE id = ?", userId);
    }

    public void updatePassword(String userId, String passwordHash) {
        this.jdbcTemplate.update("UPDATE app_users SET password_hash = ? WHERE id = ?", passwordHash, userId);
    }

    public void revokeRefreshTokensForUser(String userId) {
        this.jdbcTemplate.update("UPDATE refresh_tokens SET revoked = TRUE WHERE user_id = ?", userId);
    }

    public void deleteActionCodesForUser(String userId) {
        this.jdbcTemplate.update("DELETE FROM auth_action_tokens WHERE user_id = ?", userId);
    }

    public void deleteActionCodesForUserAndPurpose(String userId, String purpose) {
        this.jdbcTemplate.update("DELETE FROM auth_action_tokens WHERE user_id = ? AND purpose = ?",
                userId, purpose);
    }

    public Optional<RefreshAccount> refreshAccountForUpdate(String tokenHash) {

        return this.jdbcTemplate.query("""
                SELECT t.user_id, t.expires_at, t.revoked, u.email, u.email_verified
                FROM refresh_tokens t JOIN app_users u ON u.id = t.user_id
                WHERE t.token_hash = ? FOR UPDATE
                """, (rs, row) -> new RefreshAccount(
                rs.getString("user_id"), rs.getTimestamp("expires_at").toInstant(),
                rs.getBoolean("revoked"), rs.getString("email"),
                rs.getBoolean("email_verified")), tokenHash).stream().findFirst();

    }

    public void revokeRefreshToken(String tokenHash) {
        this.jdbcTemplate.update("UPDATE refresh_tokens SET revoked = TRUE WHERE token_hash = ?", tokenHash);
    }

    public void deleteExpiredRefreshTokens(Instant now) {
        this.jdbcTemplate.update("DELETE FROM refresh_tokens WHERE revoked = TRUE OR expires_at <= ?",
                Timestamp.from(now));
    }

    public void insertRefreshToken(String tokenHash, String userId, Instant expiresAt) {
        this.jdbcTemplate.update("""
                INSERT INTO refresh_tokens (token_hash, user_id, expires_at)
                VALUES (?, ?, ?)
                """, tokenHash, userId, Timestamp.from(expiresAt));
    }

    public void deleteExpiredActionCodes(Instant now) {
        this.jdbcTemplate.update("DELETE FROM auth_action_tokens WHERE expires_at <= ?", Timestamp.from(now));
    }

    public boolean hasRecentlyIssuedActionCode(
            String userId, String purpose, Instant createdAfter) {
        Integer count = this.jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM auth_action_tokens
                WHERE user_id = ? AND purpose = ? AND created_at > ?
                """, Integer.class, userId, purpose, Timestamp.from(createdAfter));
        return count != null && count > 0;
    }

    public void insertActionCode(
            String id, String userId, String purpose, String tokenHash, Instant expiresAt) {
        this.jdbcTemplate.update("""
                INSERT INTO auth_action_tokens (id, user_id, purpose, token_hash, expires_at)
                VALUES (?, ?, ?, ?, ?)
                """, id, userId, purpose, tokenHash, Timestamp.from(expiresAt));
    }

    public Optional<ActionCode> actionCodeForUpdate(String userId, String purpose) {
        return this.jdbcTemplate.query("""
                SELECT id, token_hash, attempts, expires_at
                FROM auth_action_tokens
                WHERE user_id = ? AND purpose = ?
                ORDER BY created_at DESC LIMIT 1 FOR UPDATE
                """, (rs, row) -> new ActionCode(
                rs.getString("id"), rs.getString("token_hash"), rs.getInt("attempts"),
                rs.getTimestamp("expires_at").toInstant()), userId, purpose).stream().findFirst();

    }

    public void deleteActionCode(String id) {
        this.jdbcTemplate.update("DELETE FROM auth_action_tokens WHERE id = ?", id);
    }

    public void updateActionCodeAttempts(String id, int attempts) {
        this.jdbcTemplate.update("UPDATE auth_action_tokens SET attempts = ? WHERE id = ?", attempts, id);
    }

    public record UserAccount(String id, String email, String passwordHash, boolean emailVerified) {}

    public record UserEmail(String id, String email) {}

    public record RefreshAccount(
            String userId, Instant expiresAt, boolean revoked, String email, boolean emailVerified) {}

    public record ActionCode(String id, String tokenHash, int attempts, Instant expiresAt) {}
}
