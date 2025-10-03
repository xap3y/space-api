package me.xap3y.space.entity;


import jakarta.persistence.*;
import lombok.Data;
import me.xap3y.space.api.iface.ApiResource;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;

@Entity
@Table(name = "urls")
@Data
public class Url implements ApiResource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String originalUrl;

    @Column(nullable = false)
    private String shortCode;

    @Column(nullable = false)
    @ColumnDefault("0")
    private int visits;

    @Column(nullable = false)
    @ColumnDefault("-1")
    private int maxUses;

    @Column(nullable = false)
    @ColumnDefault("CURRENT_TIMESTAMP")
    private LocalDateTime createdAt;

    @Column(nullable = true)
    private LocalDateTime expiresAt;

    @ManyToOne
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User createdBy;

    public User getUploader() {
        return this.createdBy;
    }

    public String getUniqueId() {
        return this.shortCode;
    }
}
