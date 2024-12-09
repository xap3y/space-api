package me.xap3y.space.model.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@Data
public class UIDResponse extends DefaultResponse {

    private String uniqueId;

    public UIDResponse(boolean error, String uniqueId, Object message) {
        super(error, message);
        this.uniqueId = uniqueId;
    }
}
