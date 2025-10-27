package me.xap3y.space.controller.admin;

import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.xap3y.space.api.enums.MetricRecordType;
import me.xap3y.space.api.iface.RequiresApiKey;
import me.xap3y.space.api.iface.RequiresSpecialApiKey;
import me.xap3y.space.config.ServerInfo;
import me.xap3y.space.dto.InboundEmailDto;
import me.xap3y.space.entity.TempMail;
import me.xap3y.space.entity.User;
import me.xap3y.space.handler.TempEmailWebSocketHandler;
import me.xap3y.space.mapper.InboundEmailMapper;
import me.xap3y.space.model.request.EmailRequest;
import me.xap3y.space.model.request.MissingEmailsRequest;
import me.xap3y.space.model.response.DefaultResponse;
import me.xap3y.space.service.EmailService;
import me.xap3y.space.service.InboundMailService;
import me.xap3y.space.service.PrometheusMetricService;
import me.xap3y.space.service.TempMailService;
import me.xap3y.space.util.Utils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/v1/email")
public class EmailController {

    private final EmailService emailService;
    private final ServerInfo serverInfo;
    private final TempMailService tempMailService;
    private final TempEmailWebSocketHandler tempEmailWebSocketHandler;
    private final InboundMailService inboundMailService;
    private final InboundEmailMapper inboundEmailMapper;
    private final PrometheusMetricService prometheusMetricService;

    public EmailController(ObjectProvider<EmailService> emailService, ServerInfo serverInfo, TempMailService tempMailService, TempEmailWebSocketHandler tempEmailWebSocketHandler, InboundMailService inboundMailService, InboundEmailMapper inboundEmailMapper, PrometheusMetricService prometheusMetricService) {
        this.emailService = emailService.getIfAvailable();
        this.serverInfo = serverInfo;
        this.tempMailService = tempMailService;
        this.tempEmailWebSocketHandler = tempEmailWebSocketHandler;
        this.inboundMailService = inboundMailService;
        this.inboundEmailMapper = inboundEmailMapper;
        this.prometheusMetricService = prometheusMetricService;
    }

    @PostMapping(
            value = "/send",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @RequiresSpecialApiKey
    public ResponseEntity<?> sendEmail(
            @RequestBody EmailRequest emailRequest
    ) {
        if (emailRequest.getContent() == null || emailRequest.getSubject() == null || emailRequest.getFrom() == null || emailRequest.getTo() == null) {
            return ResponseEntity.badRequest().body("Some required fields are missing");
        }

        prometheusMetricService.recordEvent(MetricRecordType.EMAIL_RECEIVED);

        emailService.sendEmail(emailRequest);

        return ResponseEntity.ok("Email sent successfully");
    }

    @PostMapping("/getmissing")
    @RequiresApiKey
    public ResponseEntity<?> getMissingMails(
            @RequestBody MissingEmailsRequest dto
    ) {
        if (dto.getMail() == null || dto.getMessageIds() == null || dto.getMessageIds().isEmpty()) {
            return ResponseEntity.badRequest().body("Missing required fields: mail or messageIds");
        }

        TempMail tempMail = tempMailService.findByEmail(dto.getMail()).orElse(null);
        if (tempMail == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Temp mail not found");
        }

        var emails = inboundMailService.getMissingEmails(tempMail, dto.getMessageIds());

        if (emails.isEmpty()) {
            return ResponseEntity.ok("No missing emails found");
        }

        List<InboundEmailDto> emailDtos = emails.stream()
                .map(inboundEmailMapper)
                .toList();

        return ResponseEntity.ok(emailDtos);
    }

    @PostMapping("/inbound")
    public ResponseEntity<?> receive(
            @RequestHeader(value = "X-Email-Token", required = false) String token,
            @RequestBody InboundEmailDto dto
    ) {
        if (!StringUtils.hasText(serverInfo.getInboundEmailToken()) || !serverInfo.getInboundEmailToken().equals(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("invalid token");
        }

        //System.out.printf("Inbound mail from=%s subject=%s%n", dto.from, dto.subject);
        log.info("Inbound mail from={} to={} subject={}", dto.from, dto.to, dto.subject);

        log.info("ENVELOPE: {}", dto.envelope);

        TempMail tempMail = tempMailService.findByEmail(dto.to).orElse(null);

        if (tempMail == null) return ResponseEntity.ok("ok");

        tempMailService.addEmailToTempMail(tempMail, dto);

        tempEmailWebSocketHandler.pushEmail(dto);

        prometheusMetricService.recordEvent(MetricRecordType.EMAIL_RECEIVED);

        log.info("Email added to temp mail: {}", tempMail.getEmail());

        return ResponseEntity.ok("ok");
    }

    @GetMapping(
            value = "/getinfo",
            produces = {
                MediaType.APPLICATION_JSON_VALUE,
                MediaType.TEXT_PLAIN_VALUE
            }
    )
    @RequiresApiKey
    public ResponseEntity<?> getTempMailInfo(
            @RequestParam("email") String email
    ) {
        TempMail tempMail = tempMailService.findByEmail(email).orElse(null);

        if (tempMail == null) {
            return new ResponseEntity<>(new DefaultResponse(true, "Temp mail not found"), HttpStatus.NOT_FOUND);
        }

        Map<String, String> res = Map.of(
                "email", tempMail.getEmail(),
                "createdAt", tempMail.getCreatedAt().toString(),
                "createdBy", tempMail.getCreatedBy().getUsername(),
                "expireAt", tempMail.getExpireAt() != null ? tempMail.getExpireAt().toString() : "never"
        );

        return new ResponseEntity<>(new DefaultResponse(false, res), HttpStatus.OK);
    }

    @PostMapping(
            value = "/create",
            produces = {
                MediaType.APPLICATION_JSON_VALUE,
                MediaType.TEXT_PLAIN_VALUE
            }
    )
    @RequiresApiKey
    public ResponseEntity<?> createTempMail(
            HttpServletRequest request
    ) {
        User creator = (User) request.getAttribute("uploader");

        String randomAddress = Utils.generateRandomId(8) + "@" + serverInfo.getInboundEmailAddress();

        TempMail tempMail = new TempMail(randomAddress, creator);

        tempMail.setExpireAt(LocalDateTime.now().plusDays(7));

        tempMailService.save(tempMail);

        prometheusMetricService.recordEvent(MetricRecordType.EMAIL_CREATED);

        log.info("Temp mail created: {} | by {}", tempMail.getEmail(), creator.getUsername());

        Map<String, String> res = Map.of(
                "email", tempMail.getEmail(),
                "createdBy", creator.getUsername(),
                "expireAt", tempMail.getExpireAt() != null ? tempMail.getExpireAt().toString() : "never"
        );

        return new ResponseEntity<>(new DefaultResponse(false, res), HttpStatus.OK);
    }
}
