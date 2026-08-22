package me.xap3y.space.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Table(name = "two_factor_login_challenges")
@Getter @Setter
public class TwoFactorLoginChallenge {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false) @JoinColumn(name = "user_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User user;
    @Column(nullable = false, unique = true, length = 64)
    private String tokenHash;
    @Column(nullable = false)
    private LocalDateTime expiresAt;
    @Column(nullable = false)
    private boolean used;
    @Column(nullable = false)
    private int failedAttempts;
}
