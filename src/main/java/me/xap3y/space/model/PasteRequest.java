package me.xap3y.space.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.springframework.lang.NonNull;

@Getter
@Setter
@AllArgsConstructor
public class PasteRequest {

    @NonNull
    private String text;

    @NonNull
    private String title;

    public PasteRequest() {}
}
