package me.xap3y.space.entity;

import jakarta.persistence.*;
import lombok.*;
import me.xap3y.space.api.enums.PortalLogType;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;

@Entity
@Table(name = "audit_log")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = true)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User userId;

    @Column(nullable = false)
    @Enumerated(EnumType.ORDINAL)
    private PortalLogType type;

    @Column(nullable = true)
    private String description;

    @Column(nullable = true)
    private String source;

    @Column(nullable = false)
    @ColumnDefault("CURRENT_TIMESTAMP")
    private LocalDateTime time;
}
