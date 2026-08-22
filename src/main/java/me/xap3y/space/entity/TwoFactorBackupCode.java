package me.xap3y.space.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "two_factor_backup_codes")
@Getter @Setter
public class TwoFactorBackupCode {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false) @JoinColumn(name = "two_factor_id", nullable = false)
    private UserTwoFactor twoFactor;
    @Column(nullable = false)
    private String codeHash;
    private LocalDateTime usedAt;
}
