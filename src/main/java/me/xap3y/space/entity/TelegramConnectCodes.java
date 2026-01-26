package me.xap3y.space.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;

@Entity
@Table(name = "telegram_connect_codes")
@Data
public class TelegramConnectCodes {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, columnDefinition = "VARCHAR(56)")
    private String code;

    @Column(nullable = false)
    @ColumnDefault("CURRENT_TIMESTAMP")
    private LocalDateTime createdAt;

    @Column(nullable = false)
    @ColumnDefault("false")
    private boolean used;

    @Column(nullable = true)
    private LocalDateTime usedAt;

    @Column(nullable = true)
    private LocalDateTime expiresAt;

    @Column(nullable = false, unique = false)
    private String fallback;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User user;
}
