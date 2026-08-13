package nus.iss.smartcart.backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Getter
@Setter
@Entity
@Table(name = "smartcart_user")
public class User {


    public User() {
        // Required by JPA - Hibernate instantiates entities via reflection when loading from the DB.
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String username;

    private String email;

    private String password;

    @Enumerated(EnumType.STRING)
    @Column(name = "role")
    private UserRole role;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private UserStatus status;

    private LocalDateTime createdAt;

    // Explicit zone (matches docker-compose's Singapore timezone) instead of the JVM's
    // implicit default, so createdAt doesn't silently shift if the host's TZ ever differs.

    //changed to PrePersist :: Junior
    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now(ZoneId.of("Asia/Singapore"));
        }
    }
}
