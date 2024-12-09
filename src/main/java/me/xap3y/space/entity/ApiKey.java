package me.xap3y.space.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.ColumnDefault;

import java.time.LocalDateTime;

@Entity
@Table(name = "api_keys")
@Data
public class ApiKey {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String keyHash;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    @ColumnDefault("0")
    private int usageCount;

    @Column(nullable = false)
    @ColumnDefault("-1")
    private int maxUploadSize;
}
