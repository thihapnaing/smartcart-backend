package nus.iss.smartcart.backend.chat.repository;

// Author: Htet Nandar (Grace)

import nus.iss.smartcart.backend.chat.model.ChatSession;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.AutoConfigureDataJpa;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Boots against the H2 in-memory test database (src/test/resources/application.properties) so
 * findBySessionId's derived query is actually exercised through Hibernate, not just mocked.
 * Mirrors the @SpringBootTest + @AutoConfigureDataJpa combo already used by
 * BackendApplicationTests, since this Spring Boot version doesn't expose a plain @DataJpaTest.
 */
@SpringBootTest
@AutoConfigureDataJpa
class ChatSessionRepositoryTest {

    @Autowired
    private ChatSessionRepository chatSessionRepository;

    @Test
    void findBySessionId_returnsTheMatchingSession() {
        chatSessionRepository.save(ChatSession.builder().sessionId("session-abc").build());

        Optional<ChatSession> found = chatSessionRepository.findBySessionId("session-abc");

        assertTrue(found.isPresent());
        assertEquals("session-abc", found.get().getSessionId());
    }

    @Test
    void findBySessionId_returnsEmptyWhenNoSessionMatches() {
        Optional<ChatSession> found = chatSessionRepository.findBySessionId("does-not-exist");

        assertTrue(found.isEmpty());
    }
}
