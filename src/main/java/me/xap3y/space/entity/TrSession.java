package me.xap3y.space.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;

@Entity
@Table(name = "tr_sessions")
@Data
@AllArgsConstructor
public class TrSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String token;

    @JoinColumn(nullable = false)
    @ManyToOne(targetEntity = MinecraftServerReports.class, fetch = FetchType.EAGER)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private MinecraftServerReports user;

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

    public TrSession() {
        this.createdAt = LocalDateTime.now();
        this.expiresAt = LocalDateTime.now().plusDays(7);
        this.lastUsedAt = null;
        this.isValid = true;
    }

    public TrSession(String token, MinecraftServerReports user, String userAgent, String ipAddress, LocalDateTime expiresAt) {
        this.token = token;
        this.user = user;
        this.userAgent = userAgent;
        this.ipAddress = ipAddress;
        this.createdAt = LocalDateTime.now();
        this.expiresAt = expiresAt;
        this.lastUsedAt = null;
        this.isValid = true;
    }
}
