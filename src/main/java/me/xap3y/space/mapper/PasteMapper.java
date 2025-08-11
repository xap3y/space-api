package me.xap3y.space.mapper;

import me.xap3y.space.config.ServerInfo;
import me.xap3y.space.dto.PasteDto;
import me.xap3y.space.dto.UrlSetDto;
import me.xap3y.space.entity.Paste;
import me.xap3y.space.util.HuffmanEncoder;
import org.springframework.stereotype.Service;

import java.util.function.Function;

@Service
public class PasteMapper implements Function<Paste, PasteDto> {

    private final ShortUserMapper shortUserMapper;
    private final ServerInfo serverInfo;
    private final HuffmanEncoder huffmanEncoder;

    public PasteMapper(ShortUserMapper shortUserMapper, ServerInfo serverInfo, HuffmanEncoder huffmanEncoder) {
        this.shortUserMapper = shortUserMapper;
        this.serverInfo = serverInfo;
        this.huffmanEncoder = huffmanEncoder;
    }

    @Override
    public PasteDto apply(Paste paste) {
        String decodedText =  huffmanEncoder.decode(paste.getContent());
        return new PasteDto(
                paste.getTitle(),
                decodedText,
                paste.isPublic(),
                paste.getCreatedAt(),
                paste.getUniqueId(),
                new UrlSetDto(
                        null,
                        serverInfo.getFrontEndUrl() + "/p/" + paste.getUniqueId(),
                        serverInfo.getBaseUrl() + "/v1/paste/get/" + paste.getUniqueId(),
                        serverInfo.getShortPasteUrl() + "/" + paste.getUniqueId(),
                        null
                ),
                shortUserMapper.apply(paste.getCreatedBy())
        );
    }
}
