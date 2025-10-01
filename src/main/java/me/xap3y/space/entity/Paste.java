package me.xap3y.space.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import me.xap3y.space.api.iface.ApiResource;
import org.hibernate.annotations.ColumnDefault;

import java.time.LocalDateTime;

@Entity
@Table(name = "pastes")
@Getter
@Setter
public class Paste implements ApiResource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String uniqueId;

    /*@Column(
            length = ConfigDb.MAX_PASTE_TEXT_LENGTH
    )
    private String content;*/

    @Column(nullable = false)
    private String title;

    @Column
    private String language;

    /*@Lob
    @Column(nullable = false, columnDefinition = "LONGBLOB")
    private byte[] content;*/

    @Lob
    @Column(nullable = false, columnDefinition = "MEDIUMTEXT")
    private String content;

    @Column(nullable = false)
    @ColumnDefault("CURRENT_TIMESTAMP")
    private LocalDateTime createdAt;

    @Column(nullable = false)
    @ColumnDefault("false")
    private boolean isPublic;

    @ManyToOne
    private User createdBy;

    public User getUploader() {
        return this.createdBy;
    }
}
