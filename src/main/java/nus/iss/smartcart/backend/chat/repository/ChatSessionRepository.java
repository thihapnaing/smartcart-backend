package nus.iss.smartcart.backend.chat.repository;

// Author: Htet Nandar (Grace)

import nus.iss.smartcart.backend.chat.model.ChatSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ChatSessionRepository extends JpaRepository<ChatSession, Long> {

    Optional<ChatSession> findBySessionId(String sessionId);
}
