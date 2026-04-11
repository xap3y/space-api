package me.xap3y.space.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;

import java.time.LocalDateTime;

@Entity
@Table(name = "minecraft_server_reports")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MinecraftServerReports {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String serverName;

    @Column(nullable = false)
    private String password;

    @Column(nullable = true)
    private String ownerIp;

    @Column(nullable = true)
    private String serverIp;

    @Column(nullable = true)
    private String ownerEmail;

    @Column(nullable = false)
    private String apiKey;

    @Column(nullable = false)
    @ColumnDefault("false")
    private boolean paused;

    @Column(nullable = false)
    @ColumnDefault("CURRENT_TIMESTAMP")
    private LocalDateTime createdAt;
}
