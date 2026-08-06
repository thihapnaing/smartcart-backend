package nus.iss.smartcart.backend.chat.model;

// Author: Htet Nandar (Grace)

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChatSessionTest {

    @Test
    void onCreate_setsCreatedAtUsingSingaporeTimezone() throws Exception {
        ChatSession session = new ChatSession();

        Method onCreate = ChatSession.class.getDeclaredMethod("onCreate");
        onCreate.setAccessible(true);
        onCreate.invoke(session);

        assertNotNull(session.getCreatedAt());
        // Compared as zone-aware ZonedDateTime (not plain LocalDateTime) per Sonar java:S6355 -
        // duration/time-difference computations should use zone-aware types.
        ZonedDateTime actual = session.getCreatedAt().atZone(ZoneId.of("Asia/Singapore"));
        ZonedDateTime expectedNow = ZonedDateTime.now(ZoneId.of("Asia/Singapore"));
        assertTrue(Duration.between(actual, expectedNow).abs().getSeconds() < 5,
            "createdAt should reflect the current time in Asia/Singapore");
    }

    @Test
    void addMessage_appendsMessageAndSetsBackReference() {
        ChatSession session = new ChatSession();
        ChatMessage message = new ChatMessage();

        session.addMessage(message);

        assertTrue(session.getMessages().contains(message));
        assertSame(session, message.getSession());
    }
}
