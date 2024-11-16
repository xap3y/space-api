package me.xap3y.space.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.lang.NonNull;

@Getter
@Setter
public class PasteRequest {

    @NonNull
    private String text;
}
