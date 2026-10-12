package com.studyapp.backend.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

@Component
public class JwtTokenService {
    private static final long TOKEN_LIFETIME_SECONDS = 15 * 60;
    private final SecretKey signingKey;

    public JwtTokenService(@Value("${studyapp.security.jwt-secret}") String secret) {
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);

        if (bytes.length < 32) {
            throw new IllegalArgumentException("JWT secret must contain at least 32 bytes.");
        }

        this.signingKey = Keys.hmacShaKeyFor(bytes);
    }

    public String createToken(AccountPrincipal principal) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(principal.id())
                .claim("email", principal.email())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(TOKEN_LIFETIME_SECONDS)))
                .signWith(this.signingKey)
                .compact();
    }

    public AccountPrincipal verify(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(this.signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return new AccountPrincipal(claims.getSubject(), claims.get("email", String.class));
    }
}
