package me.xap3y.space.model.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Map;

@Getter
@Setter
@AllArgsConstructor
public class DefaultResponse {

    private boolean error;
    private Object message;
    private LocalDateTime timestamp;
    private int count;

    public DefaultResponse(boolean error, Object message) {
        this.error = error;
        this.message = message;
        this.timestamp = LocalDateTime.now();
    }

    public DefaultResponse(boolean error, Object message, int count) {
        this.error = error;
        this.message = message;
        this.timestamp = LocalDateTime.now();
        this.count = count;
    }

    public DefaultResponse() {
        this.error = false;
        this.message = null;
        this.timestamp = LocalDateTime.now();
    }

    public Map<String, Object> toMap() {
        return Map.of("error", error, "message", message, "timestamp", timestamp);
    }

}
