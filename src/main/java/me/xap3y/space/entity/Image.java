package me.xap3y.space.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

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
    private String uniqueId;  // Random ID (8 characters)

    private String fileType;
    private long size;
    private LocalDateTime uploadTime;

    @ManyToOne
    @JoinColumn(name = "uploader_id")
    private User uploader;
}
