package me.xap3y.space.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import me.xap3y.space.api.enums.UrlSetPreference;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserUrlPreferenceSettings {
    private UrlSetPreference image;
    private UrlSetPreference paste;
    private UrlSetPreference url;
}
