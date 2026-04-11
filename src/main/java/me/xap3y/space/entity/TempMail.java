package me.xap3y.space.entity;

import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import me.xap3y.space.api.enums.TempMailStatus;
import me.xap3y.space.util.Utils;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;

@Entity
@Table(name = "temp_mails",
        indexes = {
                @Index(name = "uk_temp_mail_email", columnList = "email"),
                @Index(name = "idx_temp_mail_created_by", columnList = "created_by")
        })
@Data
@NoArgsConstructor
public class TempMail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String email;

    @ManyToOne(optional = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "created_by", nullable = true)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @Nullable
    private User createdBy;

    @Column(nullable = false)
    @ColumnDefault("CURRENT_TIMESTAMP")
    private LocalDateTime createdAt;

    @Column(nullable = false)
    @ColumnDefault("5")
    @Enumerated(EnumType.ORDINAL)
    private TempMailStatus status;

    @Column(nullable = true)
    private LocalDateTime expireAt;

    @Column(nullable = true)
    private String token;

    public TempMail(String email, User createdBy) {
        this.email = email;
        this.createdBy = createdBy;
        this.createdAt = LocalDateTime.now();
        this.status = TempMailStatus.OPEN;
        this.token = Utils.generateApiKey(32);
    }

    public String getFingerprint() {
        return id * 2 + "_" + email.split("@")[0].toLowerCase();
    }
}
