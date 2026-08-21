package me.xap3y.space.model.pcv;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PausedPackage {
    private Integer id;
    private String uuid;
    private String playerName;
    private String packageUi;
    private Long activatedAt;
    private Long pausedAt;
    private String group;
    private String displayName;
    private Integer priority;
    private Integer duration;
    private Integer packageDuration;
}
