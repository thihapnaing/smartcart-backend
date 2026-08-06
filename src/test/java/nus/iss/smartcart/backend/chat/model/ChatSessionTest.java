package nus.iss.smartcart.backend.chat.model;

// Author: Htet Nandar (Grace)

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChatSessionTest {

    @Test
    void onCreate_setsCreatedAtUsingSingaporeTimezone() throws Exception {
        ChatSession session = new ChatSession();

        Method onCreate = ChatSession.class.getDeclaredMethod("onCreate");
        onCreate.setAccessible(true);
        onCreate.invoke(session);

        assertNotNull(session.getCreatedAt());
        LocalDateTime expectedNow = LocalDateTime.now(ZoneId.of("Asia/Singapore"));
        assertTrue(Math.abs(ChronoUnit.SECONDS.between(session.getCreatedAt(), expectedNow)) < 5,
            "createdAt should reflect the current time in Asia/Singapore");
    }

    @Test
    void addMessage_appendsMessageAndSetsBackReference() {
        ChatSession session = new ChatSession();
        ChatMessage message = new ChatMessage();

        session.addMessage(message);

        assertTrue(session.getMessages().contains(message));
        assertTrue(message.getSession() == session);
    }
}
