package nus.iss.smartcart.backend.chat.model;

// Author: Htet Nandar (Grace)

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChatMessageTest {

    @Test
    void onCreate_setsCreatedAtUsingSingaporeTimezone() throws Exception {
        ChatMessage message = new ChatMessage();

        Method onCreate = ChatMessage.class.getDeclaredMethod("onCreate");
        onCreate.setAccessible(true);
        onCreate.invoke(message);

        assertNotNull(message.getCreatedAt());
        // Compared as zone-aware ZonedDateTime (not plain LocalDateTime) per Sonar java:S6355 -
        // duration/time-difference computations should use zone-aware types.
        ZonedDateTime actual = message.getCreatedAt().atZone(ZoneId.of("Asia/Singapore"));
        ZonedDateTime expectedNow = ZonedDateTime.now(ZoneId.of("Asia/Singapore"));
        assertTrue(Duration.between(actual, expectedNow).abs().getSeconds() < 5,
            "createdAt should reflect the current time in Asia/Singapore");
    }
}
