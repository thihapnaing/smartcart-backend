package nus.iss.smartcart.backend.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import nus.iss.smartcart.backend.model.User;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

//Author: Junior

@Service
public class JwtService {

    private final SecretKey secretKey;
    private final long expirationMs;

    public JwtService(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration-ms}") long expirationMs
    ) {
        this.secretKey = Keys.hmacShaKeyFor(
                secret.getBytes(StandardCharsets.UTF_8)
        );

        this.expirationMs = expirationMs;
    }

    // ==========================================================
    // Generate JWT
    // ==========================================================

    public String generateToken(User user) {

        Instant now = Instant.now();

        Instant expiration =
                now.plusMillis(expirationMs);

        return Jwts.builder()

                // EMAIL is now the JWT subject
                .subject(user.getEmail())

                .claim("userId", user.getId())

                .claim("username", user.getUsername())

                .claim("role", user.getRole().name())

                .issuedAt(java.util.Date.from(now))

                .expiration(java.util.Date.from(expiration))

                .signWith(secretKey)

                .compact();
    }

    // ==========================================================
    // Extract email
    // ==========================================================

    public String extractEmail(String token) {

        return extractClaims(token)
                .getSubject();
    }

    // ==========================================================
    // Validate JWT
    // ==========================================================

    public boolean isTokenValid(
            String token,
            User user
    ) {

        try {

            Claims claims =
                    extractClaims(token);

            return claims
                    .getSubject()
                    .equals(user.getEmail())

                    && claims
                    .getExpiration()
                    .toInstant()
                    .isAfter(Instant.now());

        } catch (Exception e) {

            return false;
        }
    }

    // ==========================================================
    // Extract claims
    // ==========================================================

    private Claims extractClaims(String token) {

        return Jwts.parser()

                .verifyWith(secretKey)

                .build()

                .parseSignedClaims(token)

                .getPayload();
    }
}