package me.xap3y.space.entity;

import jakarta.persistence.*;
import lombok.Data;
import me.xap3y.space.converter.UserUrlPreferenceConverter;
import me.xap3y.space.converter.UserWebhookSettingsConverter;
import me.xap3y.space.model.UserUrlPreferenceSettings;
import me.xap3y.space.model.UserWebhookSettings;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Table(name = "user_settings")
@Data
public class UserSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User userId;

    @Column(name = "embed_settings", nullable = false)
    @Convert(converter = UserWebhookSettingsConverter.class)
    private UserWebhookSettings embedSettings;

    @Column(name = "url_preferences", nullable = false)
    @Convert(converter = UserUrlPreferenceConverter.class)
    private UserUrlPreferenceSettings urlSettings;
}
