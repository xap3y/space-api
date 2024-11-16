package me.xap3y.space.mapper;

import me.xap3y.space.dto.PasteDto;
import me.xap3y.space.entity.Paste;
import org.springframework.stereotype.Service;

import java.util.function.Function;

@Service
public class PasteMapper implements Function<Paste, PasteDto> {

    @Override
    public PasteDto apply(Paste paste) {
        return new PasteDto(
                paste.getContent(),
                paste.isPublic(),
                paste.getCreatedAt(),
                paste.getUniqueId(),
                paste.getCreatedBy().getUsername()
        );
    }
}
