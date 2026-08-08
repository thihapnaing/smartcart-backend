package nus.iss.smartcart.backend.chat.model;

// Author: Htet Nandar (Grace)

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import nus.iss.smartcart.backend.model.User;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/**
 * A single AI-chat conversation. The business key ("sessionId") is a UUID string handed to the
 * frontend/Python service - separate from the numeric primary key - because the chat widget and
 * smartcart-ai-service already speak in terms of that UUID.
 */
@Entity
@Table(name = "chat_sessions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "messages")
public class ChatSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", nullable = false, unique = true)
    private String sessionId;

    // Nullable - anonymous/guest chat sessions (no logged-in user) are allowed.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Builder.Default
    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ChatMessage> messages = new ArrayList<>();

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    // Explicit zone (matches docker-compose's Singapore timezone) instead of the JVM's
    // implicit default, so createdAt doesn't silently shift if the host's TZ ever differs.
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now(ZoneId.of("Asia/Singapore"));
    }

    /** Convenience for appending a message while keeping both sides of the relation in sync. */
    public void addMessage(ChatMessage message) {
        messages.add(message);
        message.setSession(this);
    }
}
