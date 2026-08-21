package me.xap3y.space.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import me.xap3y.space.api.enums.UserRole;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "resource_limit_profiles",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_resource_limit_profile_role", columnNames = "role"),
                @UniqueConstraint(name = "uk_resource_limit_profile_user", columnNames = "user_id")
        })
@Getter
@Setter
public class ResourceLimitProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(length = 24)
    private UserRole role;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User user;

    @Column(nullable = false)
    private boolean pausedIndefinitely;

    private LocalDateTime pausedUntil;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
