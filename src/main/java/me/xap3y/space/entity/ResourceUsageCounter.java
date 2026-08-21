package me.xap3y.space.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import me.xap3y.space.api.enums.ResourceLimitType;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDate;

@Entity
@Table(name = "resource_usage_counters",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_resource_usage_counter",
                columnNames = {"user_id", "resource_type", "usage_date"}))
@Getter
@Setter
public class ResourceUsageCounter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "resource_type", nullable = false, length = 24)
    private ResourceLimitType resourceType;

    @Column(name = "usage_date", nullable = false)
    private LocalDate usageDate;

    @Column(nullable = false)
    private long usedCount;

    @Column(nullable = false)
    private long usedBytes;
}
