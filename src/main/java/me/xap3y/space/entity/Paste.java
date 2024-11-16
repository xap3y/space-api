package me.xap3y.space.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import me.xap3y.space.util.ConfigDb;

import java.time.LocalDateTime;

@Entity
@Table(name = "pastes")
@Getter
@Setter
public class Paste {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String uniqueId;

    /*@Column(
            length = ConfigDb.MAX_PASTE_TEXT_LENGTH
    )
    private String content;*/

    @Column(columnDefinition = "MEDIUMTEXT")
    private String content;

    private LocalDateTime createdAt;

    private boolean isPublic;

    @ManyToOne
    private User createdBy;
}
