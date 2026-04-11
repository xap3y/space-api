package me.xap3y.space.service;

import jakarta.annotation.Nullable;
import lombok.AllArgsConstructor;
import me.xap3y.space.api.enums.MetricRecordType;
import me.xap3y.space.config.ServerInfo;
import me.xap3y.space.dto.InboundEmailDto;
import me.xap3y.space.entity.InboundEmail;
import me.xap3y.space.entity.TempMail;
import me.xap3y.space.entity.User;
import me.xap3y.space.handler.TempEmailWebSocketHandler;
import me.xap3y.space.repository.TempMailRepository;
import me.xap3y.space.util.Utils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@AllArgsConstructor
public class TempMailService {

    private final TempMailRepository tempMailRepository;
    private final InboundMailService inboundMailService;
    private final ServerInfo serverInfo;
    private final PrometheusMetricService prometheusMetricService;

    public TempMail save(TempMail tempMail) {
        return tempMailRepository.save(tempMail);
    }

    public Optional<TempMail> findByEmail(String email) {
        return tempMailRepository.findByEmail(email);
    }

    public List<TempMail> findAll() {
        return tempMailRepository.findAll();
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

    public void suspendEmail(String email) {
        tempMailRepository.suspendEmail(email);
    }

    public void markDeleted(String email) {
        tempMailRepository.deleteEmail(email);
    }

    public void closeEmail(String email) {
        tempMailRepository.closeEmail(email);
    }

    public void openMail(String email) {
        tempMailRepository.openMail(email);
    }

    public TempMail createNewRandom(@Nullable User creator) {
        String randomAddress = Utils.generateRandomId(8) + "@" + serverInfo.getInboundEmailAddress();
        TempMail tempMail = new TempMail(randomAddress, creator);

        tempMail.setExpireAt(LocalDateTime.now().plusDays(7));

        prometheusMetricService.recordEvent(MetricRecordType.EMAIL_CREATED);

        return tempMailRepository.save(tempMail);
    }
}
