package me.xap3y.space.model.pcv;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class VipPackage {
    private String name;
    private String displayName;
    private Integer priority;
    private Integer duration;
    private String createdAt;
    private String group;
}

