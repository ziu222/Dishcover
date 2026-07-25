package com.dishcover.user.security;

import com.dishcover.user.config.JwtProperties;
import com.dishcover.user.entity.User;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private static final String SECRET = "test-secret-at-least-32-chars-long-000";

    private User user(long id, String plan) throws Exception {
        User u = new User("a@b.com", "hash", "An");
        Field idField = User.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(u, id);
        u.setPlan(plan);
        return u;
    }

    @Test
    void issuedTokenRoundTripsToSamePrincipal() throws Exception {
        JwtService svc = new JwtService(new JwtProperties(SECRET, 120));
        String token = svc.issue(user(42L, "PRO"));

        AuthenticatedUser parsed = svc.parse(token);
        assertEquals(42L, parsed.userId());
        assertEquals("a@b.com", parsed.email());
        assertEquals("PRO", parsed.plan());
    }

    @Test
    void tokenSignedWithAnotherSecretIsRejected() throws Exception {
        String token = new JwtService(new JwtProperties(SECRET, 120)).issue(user(1L, "FREE"));
        JwtService other = new JwtService(new JwtProperties("a-completely-different-32char-secret-x", 120));
        assertThrows(JwtException.class, () -> other.parse(token));
    }

    @Test
    void expiredTokenIsRejected() throws Exception {
        JwtService svc = new JwtService(new JwtProperties(SECRET, -1)); // hết hạn ngay
        String token = svc.issue(user(1L, "FREE"));
        assertThrows(JwtException.class, () -> svc.parse(token));
    }

    @Test
    void secretShorterThan32CharsIsRejected() {
        assertThrows(IllegalStateException.class,
                () -> new JwtService(new JwtProperties("too-short", 120)));
    }
}
