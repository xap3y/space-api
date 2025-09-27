package me.xap3y.space.service;

import lombok.AllArgsConstructor;
import me.xap3y.space.dto.InboundEmailDto;
import me.xap3y.space.entity.InboundEmail;
import me.xap3y.space.entity.TempMail;
import me.xap3y.space.repository.InboundMailRepository;
import org.springframework.stereotype.Service;

import java.util.List;
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

    public List<InboundEmail> findTop20ByTempMailOrderBySentDateDesc(TempMail tempMail) {
        return inboundMailRepository.findTop20ByTempMailOrderBySentDateDesc(tempMail);
    }

    public List<InboundEmail> getMissingEmails(TempMail tempMail, List<String> messageIds) {
        List<InboundEmail> emails = inboundMailRepository.findAllByTempMail(tempMail);
        if (emails.isEmpty()) {
            return List.of();
        }
        return emails.stream()
                .filter(email -> !messageIds.contains(email.getMessageId()))
                .toList();
    }

    public Optional<InboundEmail> findLatestByTempMail(TempMail tempMail) {
        return inboundMailRepository.findTop20ByTempMailOrderBySentDateDesc(tempMail)
                .stream()
                .findFirst();
    }
}
