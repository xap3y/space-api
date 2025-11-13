package me.xap3y.space.model.pcv;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OutWebSocketMessage {
    private String type;
    private Object data;
}
