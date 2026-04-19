package me.xap3y.space.entity;

import jakarta.persistence.*;
import lombok.Data;
import me.xap3y.space.api.enums.ImageLocation;
import me.xap3y.space.api.enums.ResourceSourceType;
import me.xap3y.space.api.iface.ApiResource;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;

@Entity
@Table(name = "files")
@Data
public class FileEntity implements ApiResource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String uniqueId;

    @Column(nullable = false)
    private String fileName;

    @Column(nullable = false)
    private String fileType;

    @Column(nullable = true)
    private String description;

    @Column(nullable = true)
    private String password;

    @Column(nullable = false)
    private long size;

    @CreationTimestamp
    @Column(nullable = false)
    private LocalDateTime uploadTime;

    @Column(nullable = true)
    private LocalDateTime expirationTime;

    @Column(nullable = true)
    private ImageLocation location;

    @Column(nullable = false)
    @ColumnDefault("0")
    private ResourceSourceType source;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = true, name = "upload_pack_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private FileUploadPack uploadPack;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = true, name = "uploader_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User uploader;
}