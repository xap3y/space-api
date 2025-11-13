package me.xap3y.space.model.pcv;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import me.xap3y.space.api.enums.PlaycoreCodeType;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PlaycoreCode {
    private PlaycoreCodeType type;
    private String code;
    private String identifier;
    private boolean used;
    private String usedBy;
    private String usedAt;
    private String generatedAt;
    private Integer duration;
    private String email;
}