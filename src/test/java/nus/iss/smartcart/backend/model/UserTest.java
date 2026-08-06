package nus.iss.smartcart.backend.model;

// Author: Htet Nandar (Grace)

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

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
        LocalDateTime expectedNow = LocalDateTime.now(ZoneId.of("Asia/Singapore"));
        assertTrue(Math.abs(ChronoUnit.SECONDS.between(user.getCreatedAt(), expectedNow)) < 5,
            "createdAt should reflect the current time in Asia/Singapore");
    }
}
