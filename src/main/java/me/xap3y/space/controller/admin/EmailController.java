package me.xap3y.space.controller.admin;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import me.xap3y.space.api.enums.MetricRecordType;
import me.xap3y.space.api.enums.PortalLogType;
import me.xap3y.space.api.enums.TempMailStatus;
import me.xap3y.space.api.exception.BadRequestException;
import me.xap3y.space.api.exception.InvalidApiKeyException;
import me.xap3y.space.api.exception.ResourceAccessForbiddenException;
import me.xap3y.space.api.exception.ResourceNotFoundException;
import me.xap3y.space.api.iface.OptionalApiKey;
import me.xap3y.space.api.iface.PathLengthValidator;
import me.xap3y.space.api.iface.RequiresApiKey;
import me.xap3y.space.api.iface.RequiresSpecialApiKey;
import me.xap3y.space.config.ServerInfo;
import me.xap3y.space.dto.InboundEmailDto;
import me.xap3y.space.dto.TempMailDto;
import me.xap3y.space.entity.InboundEmail;
import me.xap3y.space.entity.TempMail;
import me.xap3y.space.entity.User;
import me.xap3y.space.handler.TempEmailWebSocketHandler;
import me.xap3y.space.mapper.InboundEmailMapper;
import me.xap3y.space.mapper.TempMailMapper;
import me.xap3y.space.model.request.EmailRequest;
import me.xap3y.space.model.request.MissingEmailsRequest;
import me.xap3y.space.model.response.DefaultResponse;
import me.xap3y.space.service.*;
import me.xap3y.space.util.CustomLocalDateTimeDeserializer;
import me.xap3y.space.util.Utils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.*;
import org.springframework.lang.Nullable;
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
    private final TempMailMapper tempMailMapper;
    private final AuditLogService auditLogService;
    private final TurnStileService turnStileService;

    public EmailController(ObjectProvider<EmailService> emailService, ServerInfo serverInfo, TempMailService tempMailService, TempEmailWebSocketHandler tempEmailWebSocketHandler, InboundMailService inboundMailService, InboundEmailMapper inboundEmailMapper, PrometheusMetricService prometheusMetricService, TempMailMapper tempMailMapper, AuditLogService auditLogService, TurnStileService turnStileService) {
        this.emailService = emailService.getIfAvailable();
        this.serverInfo = serverInfo;
        this.tempMailService = tempMailService;
        this.tempEmailWebSocketHandler = tempEmailWebSocketHandler;
        this.inboundMailService = inboundMailService;
        this.inboundEmailMapper = inboundEmailMapper;
        this.prometheusMetricService = prometheusMetricService;
        this.tempMailMapper = tempMailMapper;
        this.auditLogService = auditLogService;
        this.turnStileService = turnStileService;
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

        prometheusMetricService.recordEvent(MetricRecordType.EMAIL_SENT);

        emailService.sendEmail(emailRequest);

        return ResponseEntity.ok("Email sent successfully");
    }

    @PostMapping("/getmissing")
    @OptionalApiKey
    public ResponseEntity<?> getMissingMails(
            HttpServletRequest request,
            @RequestBody MissingEmailsRequest dto
    ) {

        if (dto.getMail() == null || dto.getMessageIds() == null || dto.getMessageIds().isEmpty()) {
            return ResponseEntity.badRequest().body("Missing required fields: mail or messageIds");
        }

        TempMail tempMail = tempMailService.findByEmail(dto.getMail()).orElse(null);
        if (tempMail == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Temp mail not found");
        }

        User uploader = (User) request.getAttribute("uploader");
        String cookieName = "email_token_" + tempMail.getFingerprint();
        Cookie[] cookies = request.getCookies();
        boolean hasCookie = false;

        if (cookies != null) {
            for (Cookie cookie : cookies) {
                log.info("Cookie: {}={}", cookie.getName(), cookie.getValue());
                if (cookie.getName().equals(cookieName)) {
                    hasCookie = true;
                    break;
                }
            }
        }

        if (uploader == null && !hasCookie) {
            throw new InvalidApiKeyException();
        }

        var emails = inboundMailService.getMissingEmails(tempMail, dto.getMessageIds());

        if (emails.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
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

        if (dto.to.contains("netflix@xap3x.fun")) {
            log.info("Forwarding netflix email to xap3x.fun: from={} subject={}", dto.from, dto.subject);
            emailService.forwardEmail(dto);
            return ResponseEntity.ok("forwarded");
        } else {
            log.info("NOT AN NETFLIX MAIL");
        }

        TempMail tempMail = tempMailService.findByEmail(dto.to).orElse(null);

        if (tempMail == null) return ResponseEntity.ok("ok");

        InboundEmail inserted = tempMailService.addEmailToTempMail(tempMail, dto);
        InboundEmailDto insertedDto = inboundEmailMapper.apply(inserted);

        tempEmailWebSocketHandler.pushEmail(insertedDto);

        prometheusMetricService.recordEvent(MetricRecordType.EMAIL_RECEIVED);
        auditLogService.saveLog(PortalLogType.EMAIL_RECEIVE, null, tempMail.getEmail(), "API");

        log.info("Email added to temp mail: {}", tempMail.getEmail());

        return ResponseEntity.ok("ok");
    }

    @GetMapping(
            value = "/getall",
            produces = {
                    MediaType.APPLICATION_JSON_VALUE,
                    MediaType.TEXT_PLAIN_VALUE
            }
    )
    @RequiresSpecialApiKey
    public ResponseEntity<?> getTempMailInfo(
    ) {
        List<TempMail> tempMails = tempMailService.findAll();
        List<TempMailDto> tempMailDtos = tempMails.stream()
                .map(tempMailMapper)
                .toList();
        return new ResponseEntity<>(new DefaultResponse(true, tempMailDtos), HttpStatus.OK);
    }

    @GetMapping(
            value = "/getinfo",
            produces = {
                MediaType.APPLICATION_JSON_VALUE,
                MediaType.TEXT_PLAIN_VALUE
            }
    )
    @OptionalApiKey
    public ResponseEntity<?> getTempMailInfo(
            @RequestParam("email") String email,
            HttpServletRequest request
    ) {
        TempMail tempMail = tempMailService.findByEmail(email).orElse(null);

        if (tempMail == null) {
            return new ResponseEntity<>(new DefaultResponse(true, "Temp mail not found"), HttpStatus.NOT_FOUND);
        }

        User uploader = (User) request.getAttribute("uploader");

        String cookieName = "email_token_" + tempMail.getFingerprint();
        Cookie[] cookies = request.getCookies();
        boolean hasCookie = false;

        if (cookies != null) {
            for (Cookie cookie : cookies) {
                log.info("Cookie: {}={}", cookie.getName(), cookie.getValue());
                if (cookie.getName().equals(cookieName)) {
                    hasCookie = true;
                    break;
                }
            }
        }

        if (uploader == null && !hasCookie) {
            throw new InvalidApiKeyException();
        }

        Map<String, String> res = Map.of(
                "email", tempMail.getEmail(),
                "createdAt", tempMail.getCreatedAt().toString(),
                "createdBy", tempMail.getCreatedBy() != null ? tempMail.getCreatedBy().getUsername() : "public",
                "status", tempMail.getStatus().name(),
                "expireAt", tempMail.getExpireAt() != null ? tempMail.getExpireAt().toString() : "never"
        );

        return new ResponseEntity<>(new DefaultResponse(false, res), HttpStatus.OK);
    }

    @PostMapping(
            value = "/create/public",
            produces = {
                MediaType.APPLICATION_JSON_VALUE,
                MediaType.TEXT_PLAIN_VALUE
            }
    )
    public ResponseEntity<?> createTempMailPublic(
            HttpServletRequest request,
            @RequestBody(required = false) NewTempMailPublicRequest body
    ) {
        if (body == null || body.token == null || body.getToken().isBlank()) {
            throw new BadRequestException();
        }

        boolean isCaTokenValid = turnStileService.validate(body.getToken());
        if (!isCaTokenValid) throw new ResourceAccessForbiddenException();

        TempMail tempMail = tempMailService.createNewRandom(null);
        auditLogService.saveLog(PortalLogType.EMAIL_CREATE, null, tempMail.getEmail(), "API");

        log.info("Temp mail created: {} | by public API", tempMail.getEmail());

        Map<String, String> res = Map.of(
                "email", tempMail.getEmail(),
                "createdBy", "public",
                "expireAt", tempMail.getExpireAt() != null ? tempMail.getExpireAt().toString() : "never"
        );

        ResponseCookie cookie = ResponseCookie.from("email_token_" + tempMail.getFingerprint(), tempMail.getToken())
                .httpOnly(serverInfo.getSetCookieHttpOnly())
                .path("/")
                .maxAge(serverInfo.getAuthCookieMaxAge())
                .sameSite(serverInfo.getAuthCookieSameSite())
                .domain(serverInfo.getAuthCookieDomain())
                .secure(serverInfo.getAuthCookieSecure())
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(new DefaultResponse(false, res));
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

        TempMail tempMail = tempMailService.createNewRandom(creator);

        log.info("Temp mail created: {} | by {}", tempMail.getEmail(), creator.getUsername());
        auditLogService.saveLog(PortalLogType.EMAIL_CREATE, creator, tempMail.getEmail(), "API");

        Map<String, String> res = Map.of(
                "email", tempMail.getEmail(),
                "createdBy", creator.getUsername(),
                "expireAt", tempMail.getExpireAt() != null ? tempMail.getExpireAt().toString() : "never"
        );

        return new ResponseEntity<>(new DefaultResponse(false, res), HttpStatus.OK);
    }

    @PostMapping("/{email}/suspend")
    @RequiresSpecialApiKey
    @PathLengthValidator
    public ResponseEntity<?> revokeMail(
            @PathVariable("email") String email,
            HttpServletRequest request
    ) {
        //User uploader = (User) request.getAttribute("uploader");
        TempMail tempMail = tempMailService.findByEmail(email).orElse(null);

        if (tempMail == null) {
            throw new ResourceNotFoundException("Temp mail not found");
        }

        tempMailService.suspendEmail(email);
        tempEmailWebSocketHandler.closeByEmail(email);

        return new ResponseEntity<>(new DefaultResponse(false, "OK"), HttpStatus.NO_CONTENT);
    }

    @DeleteMapping("/{email}")
    @RequiresApiKey
    @PathLengthValidator
    public ResponseEntity<?> deleteMail(
            @PathVariable("email") String email,
            HttpServletRequest request
    ) {
        User uploader = (User) request.getAttribute("uploader");
        TempMail tempMail = tempMailService.findByEmail(email).orElse(null);

        if (tempMail == null) {
            throw new ResourceNotFoundException("Temp mail not found");
        } else if (!tempMail.getCreatedBy().getId().equals(uploader.getId()) && !uploader.isAdmin()) {
            throw new InvalidApiKeyException();
        }

        tempMailService.markDeleted(email);
        tempEmailWebSocketHandler.closeByEmail(email);
        auditLogService.saveLog(PortalLogType.EMAIL_DELETE, uploader, email, "API");

        return new ResponseEntity<>(new DefaultResponse(false, "OK"), HttpStatus.NO_CONTENT);
    }

    @GetMapping("/{email}/close")
    @RequiresApiKey
    @PathLengthValidator
    public ResponseEntity<?> closeMail(
            @PathVariable("email") String email,
            HttpServletRequest request
    ) {
        User uploader = (User) request.getAttribute("uploader");
        TempMail tempMail = tempMailService.findByEmail(email).orElse(null);

        if (tempMail == null) {
            throw new ResourceNotFoundException("Temp mail not found");
        } else if (!tempMail.getCreatedBy().getId().equals(uploader.getId()) && !uploader.isAdmin()) {
            throw new InvalidApiKeyException();
        }

        tempMailService.closeEmail(email);
        tempEmailWebSocketHandler.closeByEmail(email);

        return new ResponseEntity<>(new DefaultResponse(false, "OK"), HttpStatus.NO_CONTENT);
    }

    @GetMapping("/{email}/open")
    @RequiresApiKey
    @PathLengthValidator
    public ResponseEntity<?> openMail(
            @PathVariable("email") String email,
            HttpServletRequest request
    ) {
        User uploader = (User) request.getAttribute("uploader");
        TempMail tempMail = tempMailService.findByEmail(email).orElse(null);

        if (tempMail == null) {
            throw new ResourceNotFoundException("Temp mail not found");
        } else if (!tempMail.getCreatedBy().getId().equals(uploader.getId()) && !uploader.isAdmin()) {
            throw new InvalidApiKeyException();
        } else if (tempMail.getStatus().equals(TempMailStatus.OPEN)) {
            return new ResponseEntity<>(new DefaultResponse(false, "Temp mail is already open"), HttpStatus.OK);
        }

        tempMailService.openMail(email);

        return new ResponseEntity<>(new DefaultResponse(false, "OK"), HttpStatus.NO_CONTENT);
    }

    @PutMapping("/{email}/expiration")
    @RequiresSpecialApiKey
    @PathLengthValidator
    public ResponseEntity<?> updateExpiration(
            @PathVariable("email") String email,
            @RequestBody(required = true) UpdateExpirationRequest body
    ) {
        TempMail tempMail = tempMailService.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Temp mail not found"));

        if (body.expirationDate == null) {
            throw new BadRequestException("expirationDate is required");
        } else if (body.expirationDate.isBefore(LocalDateTime.now())) {
            throw new BadRequestException("expirationDate cannot be in the past");
        }

        tempMail.setExpireAt(body.expirationDate);
        tempMailService.save(tempMail);

        return new ResponseEntity<>(new DefaultResponse(false, "OK"), HttpStatus.NO_CONTENT);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record UpdateExpirationRequest(
            @JsonDeserialize(using = CustomLocalDateTimeDeserializer.class)
            @Nullable
            LocalDateTime expirationDate
    ) { }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class NewTempMailPublicRequest {
        private String token;
    }
}

