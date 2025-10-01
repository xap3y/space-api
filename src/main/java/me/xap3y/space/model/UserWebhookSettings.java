package me.xap3y.space.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserWebhookSettings {

    private Boolean enabled;
    private String color;
    private String description;
    private String title;
    private String titleUrl;
    private String authorName;

    public UserWebhookSettings(boolean enabled) {
        this.enabled = enabled;
        this.color = "RANDOM";
        this.description = null;
        this.title = null;
        this.titleUrl = null;
        this.authorName = null;
    }
}
