package me.xap3y.space.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.springframework.lang.Nullable;

@Getter
@Setter
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ShortUrlRequest {

    private String url;

    @Nullable
    private Integer maxUses;

    @Nullable
    private String uniqueId;

    public ShortUrlRequest() {}
}
