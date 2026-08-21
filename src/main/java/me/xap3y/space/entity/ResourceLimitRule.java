package me.xap3y.space.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import me.xap3y.space.api.enums.ResourceLimitPeriod;
import me.xap3y.space.api.enums.ResourceLimitType;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Table(name = "resource_limit_rules",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_resource_limit_rule",
                columnNames = {"profile_id", "resource_type", "period"}))
@Getter
@Setter
public class ResourceLimitRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "profile_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private ResourceLimitProfile profile;

    @Enumerated(EnumType.STRING)
    @Column(name = "resource_type", nullable = false, length = 24)
    private ResourceLimitType resourceType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private ResourceLimitPeriod period;

    private Long maxCount;

    private Long maxBytes;
}
