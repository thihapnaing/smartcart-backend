package nus.iss.smartcart.backend.chat.model;

// Author: Htet Nandar (Grace)

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * One message (either the user's or the assistant's) inside a {@link ChatSession}. `role` mirrors
 * the OpenAI-style values ("user" / "assistant") already used by smartcart-ai-service, so no
 * translation is needed when replaying history back to it.
 */
@Entity
@Table(name = "chat_messages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "session")
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_session_id", nullable = false)
    private ChatSession session;

    // "user" or "assistant"
    @Column(nullable = false)
    private String role;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    // Explicit zone (matches docker-compose's Singapore timezone) instead of the JVM's
    // implicit default, so createdAt doesn't silently shift if the host's TZ ever differs.
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now(ZoneId.of("Asia/Singapore"));
    }
}
