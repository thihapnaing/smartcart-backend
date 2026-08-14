package nus.iss.smartcart.backend.security;

import nus.iss.smartcart.backend.model.User;
import nus.iss.smartcart.backend.model.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtServiceTest {

    private static final String SECRET =
            "test-jwt-secret-key-not-used-for-anything-real-1234567890";

    private JwtService jwtService;
    private User user;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(SECRET, 60_000L);

        user = new User();
        user.setId(1L);
        user.setUsername("jane");
        user.setEmail("jane@example.com");
        user.setRole(UserRole.CUSTOMER);
    }

    @Test
    void generateToken_thenExtractEmail_roundTrips() {
        String token = jwtService.generateToken(user);

        assertNotNull(token);
        assertEquals("jane@example.com", jwtService.extractEmail(token));
    }

    @Test
    void isTokenValid_matchingUserAndUnexpiredToken_returnsTrue() {
        String token = jwtService.generateToken(user);

        assertTrue(jwtService.isTokenValid(token, user));
    }

    @Test
    void isTokenValid_tokenSubjectDoesNotMatchGivenUser_returnsFalse() {
        String token = jwtService.generateToken(user);

        User otherUser = new User();
        otherUser.setEmail("someone-else@example.com");

        assertFalse(jwtService.isTokenValid(token, otherUser));
    }

    @Test
    void isTokenValid_expiredToken_returnsFalse() {
        // Negative expiration puts the "expires at" timestamp in the past the instant
        // the token is minted, so JJWT rejects it as expired on parse.
        JwtService expiredTokenIssuer = new JwtService(SECRET, -60_000L);
        String expiredToken = expiredTokenIssuer.generateToken(user);

        assertFalse(jwtService.isTokenValid(expiredToken, user));
    }

    @Test
    void isTokenValid_malformedToken_returnsFalse() {
        assertFalse(jwtService.isTokenValid("not-a-real-token", user));
    }

    @Test
    void isTokenValid_tokenSignedWithDifferentSecret_returnsFalse() {
        JwtService otherJwtService =
                new JwtService("a-completely-different-secret-key-1234567890ab", 60_000L);
        String token = otherJwtService.generateToken(user);

        assertFalse(jwtService.isTokenValid(token, user));
    }
}
