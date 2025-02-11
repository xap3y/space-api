package me.xap3y.space.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.springframework.lang.Nullable;

@Getter
@Setter
@AllArgsConstructor
public class ShortUrlRequest {

    private String url;

    @Nullable
    private Integer maxUses;

    @Nullable
    private String uniqueId;

    public ShortUrlRequest() {}
}
