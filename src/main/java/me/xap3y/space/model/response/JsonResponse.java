package me.xap3y.space.model.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.Optional;

@Setter
@Getter
@AllArgsConstructor
public class JsonResponse {

    private boolean error;
    private Optional<String> uniqueId;
    private Object message;
    private Object data;

    public JsonResponse(boolean error, Object message) {
        this.error = error;
        this.message = message;
        this.uniqueId = Optional.empty();
    }

    public JsonResponse(boolean error, String uniqueId, String message) {
        this.error = error;
        this.uniqueId = Optional.of(uniqueId);
        this.message = message;
    }

    public JsonResponse(boolean error, Object message, Object data) {
        this.error = error;
        this.message = message;
        this.data = data;
        this.uniqueId = Optional.empty();
    }
}
