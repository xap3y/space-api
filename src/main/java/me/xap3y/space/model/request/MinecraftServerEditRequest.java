package me.xap3y.space.model.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.lang.Nullable;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class MinecraftServerEditRequest {

    @Nullable
    private String password;

    @Nullable
    private String ownerMail;

    @Nullable
    private String apiKey;

    @Nullable
    private Boolean paused;
}
