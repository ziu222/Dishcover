package com.dishcover.common.security;

import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtServiceTest {

    private static final String SECRET = "test-secret-at-least-32-chars-long-000";

    @Test
    void issuedTokenRoundTripsToSamePrincipal() {
        JwtService svc = new JwtService(SECRET, 120);
        String token = svc.issue(42L, "a@b.com", "PRO");

        AuthenticatedUser parsed = svc.parse(token);
        assertEquals(42L, parsed.userId());
        assertEquals("a@b.com", parsed.email());
        assertEquals("PRO", parsed.plan());
    }

    @Test
    void tokenSignedWithAnotherSecretIsRejected() {
        String token = new JwtService(SECRET, 120).issue(1L, "a@b.com", "FREE");
        JwtService other = new JwtService("a-completely-different-32char-secret-x", 120);
        assertThrows(JwtException.class, () -> other.parse(token));
    }

    @Test
    void expiredTokenIsRejected() {
        JwtService svc = new JwtService(SECRET, -1); // hết hạn ngay
        String token = svc.issue(1L, "a@b.com", "FREE");
        assertThrows(JwtException.class, () -> svc.parse(token));
    }

    @Test
    void secretShorterThan32CharsIsRejected() {
        assertThrows(IllegalStateException.class, () -> new JwtService("too-short", 120));
    }
}
