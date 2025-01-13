package me.xap3y.space.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.time.LocalDateTime;

@Entity
@Table(name = "logs")
@Getter
@Setter
public class LogRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ColumnDefault("CURRENT_TIMESTAMP")
    private LocalDateTime timestamp;

    private String ip;
    private String userAgent;
    private String path;
    private String method;
    private String result;
}
