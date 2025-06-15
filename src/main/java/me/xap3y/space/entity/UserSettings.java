package me.xap3y.space.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import me.xap3y.space.converter.UserWebhookSettingsConverter;
import me.xap3y.space.model.UserWebhookSettings;

@Entity
@Table(name = "user_settings")
@Data
public class UserSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User userId;

    @Column(name = "embed_settings", nullable = false)
    @Convert(converter = UserWebhookSettingsConverter.class)
    private UserWebhookSettings embedSettings;
}
