package me.xap3y.space.controller.admin;

import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.xap3y.space.api.iface.RequiresApiKey;
import me.xap3y.space.api.iface.RequiresSpecialApiKey;
import me.xap3y.space.config.ServerInfo;
import me.xap3y.space.dto.InboundEmailDto;
import me.xap3y.space.entity.TempMail;
import me.xap3y.space.entity.User;
import me.xap3y.space.handler.TempEmailWebSocketHandler;
import me.xap3y.space.model.AuthRegisterRequest;
import me.xap3y.space.model.EmailRequest;
import me.xap3y.space.model.response.DefaultResponse;
import me.xap3y.space.service.EmailService;
import me.xap3y.space.service.TempMailService;
import me.xap3y.space.util.Utils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/v1/email")
@AllArgsConstructor
public class EmailController {

    private final EmailService emailService;
    private final ServerInfo serverInfo;
    private final TempMailService tempMailService;
    private final TempEmailWebSocketHandler tempEmailWebSocketHandler;

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

        emailService.sendEmail(emailRequest);

        return ResponseEntity.ok("Email sent successfully");
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

        log.info("Email added to temp mail: {}", tempMail.getEmail());

        return ResponseEntity.ok("ok");
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

        String randomAddress = Utils.generateRandomId(8) + "@c.xap3y.fun";

        TempMail tempMail = new TempMail(randomAddress, creator);

        tempMail.setExpireAt(LocalDateTime.now().plusDays(7));

        tempMailService.save(tempMail);

        log.info("Temp mail created: {} | by {}", tempMail.getEmail(), creator.getUsername());

        Map<String, String> res = Map.of(
                "email", tempMail.getEmail(),
                "createdBy", creator.getUsername(),
                "expireAt", tempMail.getExpireAt() != null ? tempMail.getExpireAt().toString() : "never"
        );

        return new ResponseEntity<>(new DefaultResponse(false, res), HttpStatus.OK);
    }
}
