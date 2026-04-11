package me.xap3y.space.entity;

import jakarta.persistence.*;
import lombok.Data;
import me.xap3y.space.api.enums.ImageLocation;
import me.xap3y.space.api.enums.ResourceSourceType;
import me.xap3y.space.api.iface.ApiResource;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;

@Entity
@Table(name = "tr_images")
@Data
public class TranscriptImage implements ApiResource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String uniqueId;

    @Column(nullable = false)
    @ColumnDefault("'png'")
    private String fileType;

    @Column(nullable = false)
    private long size;

    @Column(nullable = false)
    @ColumnDefault("CURRENT_TIMESTAMP")
    private LocalDateTime uploadTime;

    @Column(nullable = false)
    @Comment("R2=0,LOCAL=1,UNKNOWN=2")
    private ImageLocation location;

    @Column(nullable = false)
    @ColumnDefault("0")
    private ResourceSourceType source;

    @ManyToOne
    @JoinColumn(nullable = true, name = "uploader_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private MinecraftServerReports uploader;

    public User getUploader() {
        return null;
    }

    public TranscriptImage() {
        this.uploadTime = LocalDateTime.now();
    }
}
