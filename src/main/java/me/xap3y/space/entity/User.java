package me.xap3y.space.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import me.xap3y.space.api.enums.UserAccountStatus;
import me.xap3y.space.api.enums.UserRole;
import me.xap3y.space.converter.UserSocialsConverter;
import me.xap3y.space.model.UserSocials;
import me.xap3y.space.util.Utils;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

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
    private String email;

    @Column(nullable = false)
    @Comment("OWNER=0,ADMIN=1,USER=2,MODERATOR=3,GUEST=4,TESTER=5,BANNED=6,DELETED=7")
    private UserRole role;

    /*@Column(unique = true, nullable = false)
    private String apiKey;*/

    @Column(nullable = true)
    @Comment("ACTIVE=0,INACTIVE=1,SUSPENDED=2,WAITING_VERIFICATION=3")
    private UserAccountStatus status;

    @ManyToOne
    @JoinColumn(unique = true, nullable = false)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    private ApiKey apiKey;

    @Column(nullable = true)
    private String avatar;

    @Column(nullable = false)
    @ColumnDefault("CURRENT_TIMESTAMP")
    private LocalDateTime createdAt;

    @ManyToOne
    @OnDelete(action = OnDeleteAction.SET_NULL)
    private User invitedBy;

    /*@ManyToOne
    @JoinColumn(name = "discord_connection_id", nullable = true)
    private DiscordConnection discordConnection;*/

    @Column(columnDefinition = "json")
    @Convert(converter = UserSocialsConverter.class)
    private UserSocials socials;

    /*@Column(columnDefinition = "json", nullable = false)
    @Convert(converter = UserStatsConverter.class)
    private UserStats stats;*/

    public User() {}

    public User(String username, String password, UserRole role, ApiKey key) {
        this.username = username;
        this.password = password;
        this.role = role;
        this.apiKey = key;
        this.status = UserAccountStatus.WAITING_VERIFICATION;
        this.createdAt = LocalDateTime.now();
        /*this.stats = new UserStats(0, 0, 0, 0, 0);*/
        this.socials = null;
        this.invitedBy = null;
    }

    public User(String email, String username, String password) {
        this.username = username;
        this.password = password;
        this.email = email;
        this.role = UserRole.USER;
        this.status = UserAccountStatus.WAITING_VERIFICATION;
        this.createdAt = LocalDateTime.now();
        /*this.stats = new UserStats(0, 0, 0, 0, 0);*/
        this.socials = null;
        this.invitedBy = null;
        this.avatar = "https://gravatar.com/avatar/" + Utils.sha256hex(email);
    }
}
