package me.xap3y.space.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;

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

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @Column(nullable = false)
    @ColumnDefault("CURRENT_TIMESTAMP")
    private LocalDateTime createdAt;

    @Column(nullable = true)
    private LocalDateTime expireAt;

    public TempMail(String email, User createdBy) {
        this.email = email;
        this.createdBy = createdBy;
        this.createdAt = LocalDateTime.now();
    }
}
