package nus.iss.smartcart.backend.chat.model;

// Author: Htet Nandar (Grace)

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

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
        LocalDateTime expectedNow = LocalDateTime.now(ZoneId.of("Asia/Singapore"));
        assertTrue(Math.abs(ChronoUnit.SECONDS.between(message.getCreatedAt(), expectedNow)) < 5,
            "createdAt should reflect the current time in Asia/Singapore");
    }
}
