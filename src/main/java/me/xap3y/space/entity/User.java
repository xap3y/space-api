package me.xap3y.space.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@Setter
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String role;

    @Column(unique = true, nullable = false)
    private String apiKey;

    private LocalDateTime createdAt;

    public User() {}

    public User(String username, String password, String role, String apiKey) {
        this.username = username;
        this.password = password;
        this.role = role;
        this.apiKey = apiKey;
        this.createdAt = LocalDateTime.now();
    }

}
