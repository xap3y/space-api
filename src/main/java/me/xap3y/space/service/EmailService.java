package me.xap3y.space.service;

import jakarta.mail.internet.MimeMessage;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import me.xap3y.space.api.enums.MetricRecordType;
import me.xap3y.space.config.ServerInfo;
import me.xap3y.space.model.request.EmailRequest;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
@ConditionalOnProperty(name = "USE_DISCORD_BOT", havingValue = "true", matchIfMissing = false)
public class EmailService {

    private final ServerInfo serverInfo;
    private final PrometheusMetricService prometheusMetricService;
    private JavaMailSender mailSender;

    public void sendVerificationCode(String to, String code, String urlCode) {

        if (to == null || to.isEmpty() || code == null || code.isEmpty()) {
            throw new IllegalArgumentException("Email address and verification code must not be null or empty");
        }

        String url = serverInfo.getBaseUrl();

        EmailRequest emailRequest = new EmailRequest();
        emailRequest.setFrom("root@xap3y.space");
        emailRequest.setTo(to);
        emailRequest.setSubject("Your Verification Code - " + code);
        emailRequest.setContent("Your code is: " + code + "\n\n" +
                "Or you can verify your account using this link below:\n" +
                url + "/v1/auth/verify/token?code=" + urlCode + "\n\n" +
                "If you did not request this, please ignore this email.");

        sendEmail(emailRequest);
    }

    public void sendEmail(EmailRequest emailRequest) {

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(emailRequest.getFrom());
        message.setTo(emailRequest.getTo());
        message.setSubject(emailRequest.getSubject());
        message.setText(emailRequest.getContent());

        prometheusMetricService.recordEvent(MetricRecordType.EMAIL_SENT);

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
