package me.xap3y.space.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Comment;

import java.time.LocalDateTime;

@Entity
@Table(name = "images")
@Getter
@Setter
public class Image {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String uniqueId;

    @Column(nullable = false)
    @ColumnDefault("'png'")
    private String fileType;

    @Column(nullable = true)
    private String description;

    @Column(nullable = true)
    private String password;

    @Column(nullable = false)
    private long size;

    @Column(nullable = false)
    @ColumnDefault("CURRENT_TIMESTAMP")
    private LocalDateTime uploadTime;

    @Column(nullable = true)
    private LocalDateTime expirationTime;

    @Column(nullable = false)
    @ColumnDefault("true")
    private Boolean isPublic;

    @ManyToOne
    @JoinColumn(name = "uploader_id")
    private User uploader;
}
