package me.xap3y.space.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import me.xap3y.space.api.enums.ImageLocation;
import me.xap3y.space.api.iface.ApiResource;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;

@Entity
@Table(name = "images")
@Getter
@Setter
public class Image implements ApiResource {

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
    private boolean isPublic;

    @Column(nullable = false)
    @ColumnDefault("false")
    private boolean poster;

    @Column(nullable = false)
    @Comment("R2=0,LOCAL=1,UNKNOWN=2")
    private ImageLocation location;

    @ManyToOne
    @JoinColumn(nullable = true, name = "uploader_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User uploader;
}
