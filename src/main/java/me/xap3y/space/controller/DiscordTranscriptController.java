package me.xap3y.space.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import me.xap3y.space.api.exception.BadRequestException;
import me.xap3y.space.api.exception.ResourceNotFoundException;
import me.xap3y.space.api.iface.OptionalCookieAuth;
import me.xap3y.space.api.iface.PathLengthValidator;
import me.xap3y.space.api.iface.RequiresApiKey;
import me.xap3y.space.model.DiscordTranscript;
import me.xap3y.space.model.response.UIDResponse;
import me.xap3y.space.service.DiscordTranscriptService;
import me.xap3y.space.util.Utils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/v1/discord/transcript")
public class DiscordTranscriptController {

    private final DiscordTranscriptService discordTranscriptService;

    public DiscordTranscriptController(DiscordTranscriptService discordTranscriptService) {
        this.discordTranscriptService = discordTranscriptService;
    }

    @PostMapping("/upload")
    @RequiresApiKey
    public ResponseEntity<?> saveTranscript(
            HttpServletRequest request,
            @RequestBody(required = false) DiscordTranscript body
    ) {
        if (body == null) {
            throw new BadRequestException("Request body is missing or invalid");
        }

        // save json to file
        String uniqueId = Utils.generateRandomId(10);
        discordTranscriptService.saveToFile(body, uniqueId);

        return new ResponseEntity<>(new UIDResponse(false, uniqueId, "Saved."), HttpStatus.OK);
    }

    @GetMapping("/get/{uniqueId}")
    @RequiresApiKey
    @OptionalCookieAuth
    @PathLengthValidator
    public ResponseEntity<?> getTranscript(
            HttpServletRequest request,
            @PathVariable String uniqueId
    ) {
        DiscordTranscript transcript = discordTranscriptService.loadFromFile(uniqueId);
        if (transcript == null) {
            throw new ResourceNotFoundException();
        }

        return new ResponseEntity<>(new UIDResponse(false, uniqueId, transcript), HttpStatus.OK);
    }
}
