package com.rks.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtServiceTest {

    @Test
    void shouldGenerateAndValidateTokenWithShortSecret() throws Exception {
        JwtService jwtService = new JwtService();
        setField(jwtService, "secretKey", "short-jwt-secret-12345678");
        setField(jwtService, "jwtExpiration", 60000L);

        jwtService.init();

        UserDetails userDetails = User.withUsername("admin@rks.com")
                .password("ignored")
                .roles("ADMIN")
                .build();

        String token = jwtService.generateToken(userDetails);

        assertNotNull(token);
        assertTrue(jwtService.isTokenValid(token));
        assertEquals("admin@rks.com", jwtService.extractEmail(token));
        assertFalse(jwtService.isTokenValid(token + "tampered"));
    }

    @Test
    void shouldRejectBlankSecret() throws Exception {
        JwtService jwtService = new JwtService();
        setField(jwtService, "secretKey", "   ");

        IllegalStateException erro = assertThrows(IllegalStateException.class, jwtService::init);
        assertTrue(erro.getMessage().contains("JWT_SECRET"));
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}