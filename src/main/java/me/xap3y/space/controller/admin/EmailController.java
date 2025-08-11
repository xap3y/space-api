package me.xap3y.space.controller.admin;

import lombok.AllArgsConstructor;
import me.xap3y.space.api.iface.RequiresSpecialApiKey;
import me.xap3y.space.model.AuthRegisterRequest;
import me.xap3y.space.model.EmailRequest;
import me.xap3y.space.service.EmailService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/email")
@AllArgsConstructor
public class EmailController {

    private final EmailService emailService;

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
}
