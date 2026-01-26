package me.xap3y.space.mapper;

import lombok.extern.slf4j.Slf4j;
import me.xap3y.space.config.ServerInfo;
import me.xap3y.space.dto.PasteDto;
import me.xap3y.space.dto.UrlSetDto;
import me.xap3y.space.entity.Paste;
import me.xap3y.space.util.HuffmanEncoder;
import org.springframework.stereotype.Service;

import java.util.function.Function;

@Slf4j
@Service
public class PasteMapper implements Function<Paste, PasteDto> {

    private final ShortUserMapper shortUserMapper;
    private final ServerInfo serverInfo;
    private final HuffmanEncoder huffmanEncoder;
    private final UrlSetMapper urlSetMapper;

    public PasteMapper(ShortUserMapper shortUserMapper, ServerInfo serverInfo, HuffmanEncoder huffmanEncoder, UrlSetMapper urlSetMapper) {
        this.shortUserMapper = shortUserMapper;
        this.serverInfo = serverInfo;
        this.huffmanEncoder = huffmanEncoder;
        this.urlSetMapper = urlSetMapper;
    }

    @Override
    public PasteDto apply(Paste paste) {
        //log.info("STARTING TO DECODE");
        //String decodedText =  huffmanEncoder.decode(paste.getContent());
        //log.info("DECODED");
        return new PasteDto(
                paste.getTitle(),
                paste.getContent() != null ? paste.getContent() : "",
                paste.isPublic(),
                paste.getCreatedAt(),
                paste.getUniqueId(),
                urlSetMapper.apply(paste),
                shortUserMapper.apply(paste.getCreatedBy())
        );
    }

    public PasteDto apply(Paste paste, boolean includeContent) {
        //log.info("STARTING TO DECODE");
        //String decodedText =  huffmanEncoder.decode(paste.getContent());
        //log.info("DECODED");
        return new PasteDto(
                paste.getTitle(),
                includeContent ? paste.getContent() : null,
                paste.isPublic(),
                paste.getCreatedAt(),
                paste.getUniqueId(),
                urlSetMapper.apply(paste),
                shortUserMapper.apply(paste.getCreatedBy())
        );
    }
}
