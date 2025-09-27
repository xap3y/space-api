package me.xap3y.space.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.ColumnDefault;

import java.time.LocalDateTime;

@Entity
@Table(name = "sessions")
@Data
public class Sessions {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String token;

    @JoinColumn(nullable = false)
    @ManyToOne(targetEntity = User.class, fetch = FetchType.EAGER)
    private User userId;

    @Column(nullable = false)
    @ColumnDefault("CURRENT_TIMESTAMP")
    private LocalDateTime createdAt;

    @Column(nullable = true)
    private LocalDateTime lastUsedAt;

    @Column(nullable = false)
    @ColumnDefault("CURRENT_TIMESTAMP")
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    @ColumnDefault("true")
    private Boolean isValid;

    @Column(nullable = false)
    private String userAgent;

    @Column(nullable = false)
    private String ipAddress;

    public Sessions(String token, User user, String userAgent, String ipAddress, LocalDateTime expiresAt) {
        this.token = token;
        this.userId = user;
        this.userAgent = userAgent;
        this.ipAddress = ipAddress;
        this.createdAt = LocalDateTime.now();
        this.expiresAt = expiresAt;
        this.lastUsedAt = null;
        this.isValid = true;
    }

    public Sessions() {
        this.createdAt = LocalDateTime.now();
        this.expiresAt = LocalDateTime.now().plusDays(7);
        this.lastUsedAt = null;
        this.isValid = true;
    }
}
