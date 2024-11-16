package me.xap3y.space.entity;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "urls")
@Getter
@Setter
public class Url {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String originalUrl;
    private String shortCode;
    private int visits;

    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;

    @ManyToOne
    private User createdBy;

}
