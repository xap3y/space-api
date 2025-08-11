package me.xap3y.space.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "telegram_connections")
@Data
@NoArgsConstructor
public class TelegramConnection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User userId;

    @Column(nullable = false, unique = true)
    private String telegramId;

    @Column(nullable = true)
    private String accessToken;

    @ManyToOne
    @JoinColumn(name = "email_verify_code_id", nullable = true)
    private EmailVerifyCodes emailVerifyCode;

    public TelegramConnection(User user, String telegramId, String accessToken) {
        this.userId = user;
        this.telegramId = telegramId;
        this.accessToken = accessToken;
    }

    public TelegramConnection(User user, String telegramId, EmailVerifyCodes emailVerifyCode) {
        this.userId = user;
        this.telegramId = telegramId;
        this.emailVerifyCode = emailVerifyCode;
    }
}
