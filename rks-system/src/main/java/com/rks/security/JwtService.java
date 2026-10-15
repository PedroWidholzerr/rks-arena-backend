package com.rks.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;

@Service
@Lazy(false)
public class JwtService {
    private static final Logger logger = LoggerFactory.getLogger(JwtService.class);
    private static final int MINIMUM_HMAC_KEY_LENGTH_BYTES = 32;

    @Value("${jwt.secret:}")
    private String secretKey;

    @Value("${jwt.expiration:86400000}")
    private long jwtExpiration;

    private SecretKey key;

    @PostConstruct
    public void init() {
        this.key = Keys.hmacShaKeyFor(resolveSigningKey(secretKey));
    }

    private byte[] resolveSigningKey(String configuredSecret) {
        if (configuredSecret == null || configuredSecret.isBlank()) {
            throw new IllegalStateException("JWT_SECRET não definido: configure a variável de ambiente JWT_SECRET (sem valor padrão, mínimo recomendado de 32 caracteres).");
        }

        byte[] secretBytes = configuredSecret.getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length >= MINIMUM_HMAC_KEY_LENGTH_BYTES) {
            return secretBytes;
        }

        logger.warn("JWT secret shorter than {} bytes detected. Deriving a SHA-256 key from the configured secret.", MINIMUM_HMAC_KEY_LENGTH_BYTES);
        try {
            return MessageDigest.getInstance("SHA-256").digest(secretBytes);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 algorithm is not available", exception);
        }
    }

    public String generateToken(UserDetails userDetails) {
        return generateToken(userDetails.getUsername());
    }

    public String generateToken(String email) {
        return Jwts.builder()
                .subject(email)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + jwtExpiration))
                .signWith(key)
                .compact();
    }

    public String extractEmail(String token) {
        return extractClaims(token).getSubject();
    }

    public boolean isTokenValid(String token) {
        try {
            Jwts.parser()
                    .keyLocator(header -> key)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private Claims extractClaims(String token) {
        return Jwts.parser()
                .keyLocator(header -> key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
