package me.xap3y.space.service;

import jakarta.mail.internet.MimeMessage;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import me.xap3y.space.api.enums.MetricRecordType;
import me.xap3y.space.config.ServerInfo;
import me.xap3y.space.dto.InboundEmailDto;
import me.xap3y.space.model.request.EmailRequest;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@ConditionalOnProperty(name = "USE_DISCORD_BOT", havingValue = "true", matchIfMissing = false)
public class EmailService {

    private final ServerInfo serverInfo;
    private final PrometheusMetricService prometheusMetricService;
    private final JavaMailSender mailSender;
    private final JavaMailSender netflixMailSender;

    public EmailService(
            ServerInfo serverInfo,
            PrometheusMetricService prometheusMetricService,
            @Qualifier("primaryMailSender") JavaMailSender mailSender,
            @Qualifier("secondaryMailSender") JavaMailSender netflixMailSender
    ) {
        this.serverInfo = serverInfo;
        this.prometheusMetricService = prometheusMetricService;
        this.mailSender = mailSender;
        this.netflixMailSender = netflixMailSender;
    }

    public void sendVerificationCode(String to, String code, String urlCode) {

        if (to == null || to.isEmpty() || code == null || code.isEmpty()) {
            throw new IllegalArgumentException("Email address and verification code must not be null or empty");
        }

        String url = serverInfo.getBaseUrl();

        EmailRequest emailRequest = new EmailRequest();
        emailRequest.setFrom("auth@xap3y.space");
        emailRequest.setTo(to);
        emailRequest.setSubject("Your Verification Code - " + code);
        emailRequest.setContent("Your code is: " + code + "\n\n" +
                "Or you can verify your account using this link below:\n" +
                url + "/v1/auth/verify/token?code=" + urlCode + "\n\n" +
                "If you did not request this, please ignore this email.");

        sendEmail(emailRequest);
    }

    public void forwardEmail(InboundEmailDto dto) {
        String matty = "hoskova.matyas@gmail.com";
        String xap3y = "minecubeks@gmail.com";
        String sky = "shweyeewin496@gmail.com";

        if (dto.getSubject().toLowerCase().contains("kód") || dto.getSubject().toLowerCase().contains("code")) {
            log.info("Forwarding code email: {}", dto.getSubject());
            netflixMailSender.send(buildFor(xap3y, dto));
            netflixMailSender.send(buildFor(sky, dto));
            netflixMailSender.send(buildFor(matty, dto));
        } else {
            log.info("Forwarding OTHER netflix email: {}", dto.getSubject());
            netflixMailSender.send(buildFor(matty, dto));
        }
    }

    private MimeMessage buildFor(String receiver, InboundEmailDto dto) {

        MimeMessage message = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setFrom("netflix@xap3y.space");
            helper.setTo(receiver);
            helper.setSubject("Fwd: " + dto.getSubject());
            helper.setText(dto.getHtml(), true);
            return message;
        } catch (Exception e) {
            log.error("Failed to build email for forwarding", e);
            throw new RuntimeException("Failed to build email for forwarding", e);
        }
    }

    public void sendEmail(EmailRequest emailRequest) {

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(emailRequest.getFrom());
        message.setTo(emailRequest.getTo());
        message.setSubject(emailRequest.getSubject());
        message.setText(emailRequest.getContent());

        prometheusMetricService.recordEvent(MetricRecordType.EMAIL_SENT);

        log.info("Sending email to: {}, Subject: {}", emailRequest.getTo(), emailRequest.getSubject());

        mailSender.send(message);
    }

    @SneakyThrows
    public void sendComplexEmail(EmailRequest req) {
        MimeMessage message = mailSender.createMimeMessage();

        MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");

        helper.setFrom(req.getFrom());
        helper.setTo(req.getTo());
        helper.setSubject(req.getSubject());
        helper.setText(req.getContent(), true);

        mailSender.send(message);
    }
}
