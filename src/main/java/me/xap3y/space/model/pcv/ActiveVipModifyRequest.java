package me.xap3y.space.model.pcv;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ActiveVipModifyRequest {
    private String uuid;
    private String identifier;
    private Integer duration;
    private String playerName;
}
