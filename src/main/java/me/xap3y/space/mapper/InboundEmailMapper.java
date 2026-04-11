package me.xap3y.space.mapper;

import me.xap3y.space.dto.InboundEmailDto;
import me.xap3y.space.entity.InboundEmail;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Service
public class InboundEmailMapper implements Function<InboundEmail, InboundEmailDto> {

    @Override
    public InboundEmailDto apply(InboundEmail mail) {
        return new InboundEmailDto(
                new InboundEmailDto.Envelope(),
                Map.of(),
                mail.getSubject(),
                mail.getFromAddress(),
                mail.getToAddresses(),
                mail.getCcAddresses(),
                mail.getSentDate().toString(),
                mail.getMessageId(),
                mail.getTextBody(),
                mail.getHtmlBody(),
                List.of(),
                mail.getId()
        );
    }
}
