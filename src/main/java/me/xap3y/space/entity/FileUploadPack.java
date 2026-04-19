package me.xap3y.space.entity;

import jakarta.persistence.*;
import lombok.Data;
import me.xap3y.space.api.enums.ResourceSourceType;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "file_upload_packs")
@Data
public class FileUploadPack {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String packId;

    @CreationTimestamp
    @Column(nullable = false)
    private LocalDateTime uploadTime;

    @Column(nullable = true)
    private String description;

    @Column(nullable = true)
    private String password;

    @Column(nullable = false)
    @ColumnDefault("0")
    private long totalSize;

    @Column(nullable = false)
    @ColumnDefault("0")
    private ResourceSourceType source;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = true, name = "uploader_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User uploader;

    @OneToMany(mappedBy = "uploadPack", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<FileEntity> files = new ArrayList<>();

    @Column(nullable = false)
    @ColumnDefault("false")
    private boolean isComplete;
}