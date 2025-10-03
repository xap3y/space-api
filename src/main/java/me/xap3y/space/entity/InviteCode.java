package me.xap3y.space.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;

@Entity
@Table(name = "invite_codes")
@Getter
@Setter
public class InviteCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String code;

    @Column(nullable = false)
    @ColumnDefault("false")
    private boolean used;

    @Column(nullable = false)
    @ColumnDefault("CURRENT_TIMESTAMP")
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime usedAt;

    @ManyToOne
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User createdBy;

    @ManyToOne
    private User usedBy;

    public InviteCode(String code) {
        this.code = code;
        this.createdAt = LocalDateTime.now();
        this.used = false;
    }

    public InviteCode() {}
}
