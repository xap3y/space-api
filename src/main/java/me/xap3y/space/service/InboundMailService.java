package me.xap3y.space.service;

import lombok.AllArgsConstructor;
import me.xap3y.space.dto.InboundEmailDto;
import me.xap3y.space.entity.InboundEmail;
import me.xap3y.space.entity.TempMail;
import me.xap3y.space.repository.InboundMailRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@AllArgsConstructor
public class InboundMailService {

    private final InboundMailRepository inboundMailRepository;

    public Optional<InboundEmail> findByTempMail(TempMail email) {
        return inboundMailRepository.findByTempMail(email);
    }

    public Optional<InboundEmail> findByTempMailId(Long tempMailId) {
        return inboundMailRepository.findByTempMail_Id(tempMailId);
    }

    public Optional<InboundEmail> findByTempMailEmail(String tempMailEmail) {
        return inboundMailRepository.findByTempMail_Email(tempMailEmail);
    }

    public InboundEmail save(InboundEmail email) {
        return inboundMailRepository.save(email);
    }

    public InboundEmail save(TempMail mail, InboundEmailDto dto) {
        InboundEmail email = new InboundEmail(mail, dto);
        return inboundMailRepository.save(email);
    }
}
