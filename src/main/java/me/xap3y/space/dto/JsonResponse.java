package me.xap3y.space.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
public class JsonResponse {

    private boolean error;
    private String uniqueId;
    private Object message;
    private Object data;

    public JsonResponse(boolean error, Object message) {
        this.error = error;
        this.message = message;
        this.uniqueId = null;
    }

    public JsonResponse(boolean error, String uniqueId, String message) {
        this.error = error;
        this.uniqueId = uniqueId;
        this.message = message;
    }

    public JsonResponse(boolean error, Object message, Object data) {
        this.error = error;
        this.message = message;
        this.data = data;
    }
}
