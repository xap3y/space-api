package me.xap3y.space.model.pcv;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ActivePackage {
    private String playerName;
    private String playerUniqueId;
    private String packageName;
    private int duration;
    private String activatedAt;
}