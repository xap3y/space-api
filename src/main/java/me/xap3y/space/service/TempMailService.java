package me.xap3y.space.service;

import lombok.AllArgsConstructor;
import me.xap3y.space.dto.InboundEmailDto;
import me.xap3y.space.entity.InboundEmail;
import me.xap3y.space.entity.TempMail;
import me.xap3y.space.repository.TempMailRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@AllArgsConstructor
public class TempMailService {

    private final TempMailRepository tempMailRepository;
    private final InboundMailService inboundMailService;

    public TempMail save(TempMail tempMail) {
        return tempMailRepository.save(tempMail);
    }

    public Optional<TempMail> findByEmail(String email) {
        return tempMailRepository.findByEmail(email);
    }

    public boolean existsByEmail(String email) {
        return tempMailRepository.existsByEmail(email);
    }

    public InboundEmail addEmailToTempMail(TempMail tempMail, InboundEmailDto email) {
        if (tempMail != null && email != null) {
            return inboundMailService.save(tempMail, email);
        }
        return null;
    }
}
