package me.xap3y.space.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import me.xap3y.space.util.Utils;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;

@Entity
@Table(name = "email_verify_codes")
@Data
@NoArgsConstructor
public class EmailVerifyCodes {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, columnDefinition = "VARCHAR(8)")
    private String code;

    @Column(nullable = false, unique = true, columnDefinition = "VARCHAR(56)")
    private String urlCode;

    @Column(nullable = false, unique = true, columnDefinition = "VARCHAR(56)")
    private String telCode;

    @Column(nullable = false)
    @ColumnDefault("CURRENT_TIMESTAMP")
    private LocalDateTime createdAt;

    @Column(nullable = true)
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    private String email;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User user;

    @Column(nullable = false)
    @ColumnDefault("false")
    private boolean used;

    public EmailVerifyCodes(String code, User user) {
        this.code = code;
        this.urlCode = Utils.generateRandomId(26);
        this.telCode = Utils.generateRandomId(30);
        this.email = user.getEmail();
        this.user = user;
        this.createdAt = LocalDateTime.now();
        this.expiresAt = LocalDateTime.now().plusHours(8);
    }
}
