package nus.iss.smartcart.backend.model;

// Author: Htet Nandar (Grace)

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserTest {

    @Test
    void onCreate_setsCreatedAtUsingSingaporeTimezone() throws Exception {
        User user = new User();

        Method onCreate = User.class.getDeclaredMethod("onCreate");
        onCreate.setAccessible(true);
        onCreate.invoke(user);

        assertNotNull(user.getCreatedAt());
        // Compared as zone-aware ZonedDateTime (not plain LocalDateTime) per Sonar java:S6355 -
        // duration/time-difference computations should use zone-aware types.
        ZonedDateTime actual = user.getCreatedAt().atZone(ZoneId.of("Asia/Singapore"));
        ZonedDateTime expectedNow = ZonedDateTime.now(ZoneId.of("Asia/Singapore"));
        assertTrue(Duration.between(actual, expectedNow).abs().getSeconds() < 5,
            "createdAt should reflect the current time in Asia/Singapore");
    }
}
