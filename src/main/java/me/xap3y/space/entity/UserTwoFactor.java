package me.xap3y.space.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Table(name = "user_two_factor")
@Getter @Setter
public class UserTwoFactor {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @OneToOne(optional = false) @JoinColumn(name = "user_id", nullable = false, unique = true)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User user;
    @Column(nullable = false, length = 512)
    private String encryptedSecret;
    @Column(nullable = false)
    private boolean enabled;
    private LocalDateTime enabledAt;
    @OneToMany(mappedBy = "twoFactor", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TwoFactorBackupCode> backupCodes = new ArrayList<>();
}
