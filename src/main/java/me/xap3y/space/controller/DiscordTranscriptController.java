package me.xap3y.space.controller;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import me.xap3y.space.api.enums.PortalLogType;
import me.xap3y.space.api.exception.BadRequestException;
import me.xap3y.space.api.exception.ResourceAccessForbiddenException;
import me.xap3y.space.api.exception.ResourceNotFoundException;
import me.xap3y.space.api.iface.*;
import me.xap3y.space.config.ServerInfo;
import me.xap3y.space.entity.DiscordReportTranscript;
import me.xap3y.space.entity.MinecraftServerReports;
import me.xap3y.space.model.DiscordTranscript;
import me.xap3y.space.model.request.MinecraftServerEditRequest;
import me.xap3y.space.model.response.DefaultResponse;
import me.xap3y.space.model.response.UIDResponse;
import me.xap3y.space.service.*;
import me.xap3y.space.util.Utils;
import me.xap3y.space.util.XorCodec;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/v1/discord/transcript")
@AllArgsConstructor
public class DiscordTranscriptController {

    private final DiscordTranscriptService discordTranscriptService;
    private final MinecraftServerReportsService minecraftServerReportsService;
    private final ServerInfo serverInfo;
    private final TurnStileService turnStileService;
    private final AuditLogService auditLogService;
    private final DiscordReportTranscriptService discordReportTranscriptService;
    private final ObjectMapper objectMapper;

    @PostMapping("/upload")
    @RequiresMcApiKey
    public ResponseEntity<?> saveTranscript(
            HttpServletRequest request,
            @RequestBody(required = false) DiscordTranscript body
    ) {
        if (body == null) {
            throw new BadRequestException("Request body is missing or invalid");
        }

        // save json to file
        String uniqueId = Utils.generateRandomId(10);
        //discordTranscriptService.saveToFile(body, uniqueId);
        DiscordReportTranscript discordReportTranscript = new DiscordReportTranscript();
        MinecraftServerReports uploader = (MinecraftServerReports) request.getAttribute("minecraftServerReport");
        if (uploader.isPaused()) {
            throw new ResourceAccessForbiddenException("Your server is paused, you cannot upload transcripts until it is unpaused.");
        }
        discordReportTranscript.setUser(uploader);
        discordReportTranscript.setUniqueId(uniqueId);
        try {
            discordReportTranscript.setContent(objectMapper.writeValueAsString(body));
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize transcript content: {}", e.getMessage());
            throw new BadRequestException("Failed to process transcript content");
        }
        discordReportTranscript.setCreatedAt(LocalDateTime.now());

        discordReportTranscriptService.save(discordReportTranscript);

        return new ResponseEntity<>(new UIDResponse(false, uniqueId, "OK"), HttpStatus.OK);
    }

    @GetMapping("/getall/users")
    @RequiresSpecialApiKey
    public ResponseEntity<?> getAllusers(
            HttpServletRequest request
    ) {
        List<MinecraftServerReports> allReports = minecraftServerReportsService.findAll();
        return new ResponseEntity<>(new DefaultResponse(false, allReports, allReports.size()), HttpStatus.OK);
    }

    @DeleteMapping("/get/{serverId}")
    @RequiresSpecialApiKey
    @PathLengthValidator
    public ResponseEntity<?> deleteServer(
            HttpServletRequest request,
            @PathVariable String serverId
    ) {
        Optional<MinecraftServerReports> reportServer = minecraftServerReportsService.findByServerName(serverId);
        if (reportServer.isEmpty()) {
            throw new ResourceNotFoundException();
        }
        minecraftServerReportsService.deleteById(reportServer.get().getId());
        return new ResponseEntity<>(new DefaultResponse(false, "OK"), HttpStatus.NO_CONTENT);
    }

    @PutMapping("/get/{serverId}")
    @RequiresSpecialApiKey
    @PathLengthValidator
    public ResponseEntity<?> updateServer(
            HttpServletRequest request,
            @PathVariable String serverId,
            @RequestBody(required = false) MinecraftServerEditRequest body
    ) {
        if (body == null) {
            throw new BadRequestException();
        }
        Optional<MinecraftServerReports> reportServer = minecraftServerReportsService.findByServerName(serverId);
        if (reportServer.isEmpty()) {
            throw new ResourceNotFoundException();
        }
        if (body.getApiKey() != null && !body.getApiKey().isBlank()) {
            reportServer.get().setApiKey(body.getApiKey());
        }
        if (body.getOwnerMail() != null) {
            if (body.getOwnerMail().equals("null")) {
                reportServer.get().setOwnerEmail(null);
            } else {
                if (body.getOwnerMail().length() > 100) throw new BadRequestException("Owner email is too long (max 100 characters)");
                reportServer.get().setOwnerEmail(body.getOwnerMail());
            }
        }
        if (body.getPassword() != null) {
            if (body.getPassword().length() < 4) throw new BadRequestException("Password is too short (min 4 characters)");
            reportServer.get().setPassword(body.getPassword());
        }
        if (body.getPaused() != null) {
            reportServer.get().setPaused(body.getPaused());
        }
        minecraftServerReportsService.save(reportServer.get());
        return new ResponseEntity<>(new DefaultResponse(false, "OK"), HttpStatus.NO_CONTENT);
    }

    @PostMapping("/create")
    @RequiresSpecialApiKey
    public ResponseEntity<?> createKey(
            HttpServletRequest request,
            @RequestBody(required = false) DiscordTranscriptController.NewMinecraftServerReportRequest body
    ) {
        if (body == null) {
            throw new BadRequestException("Request body is missing or invalid");
        }

        if (body.serverName == null) throw new BadRequestException("serverName is required");
        else if (body.getToken() == null || body.getToken().isBlank()) throw new BadRequestException("token is required");
        else if (body.getServerName() != null && body.getServerName().length() > 30) throw new BadRequestException("Server name is too long (max 30 characters)");
        else if (body.getServerName() != null && body.getServerName().length() < 4) throw new BadRequestException("Server name is too short (min 4 characters)");
        else if (body.getPassword() == null) throw new BadRequestException("Password is required");
        else if (body.getPassword().length() < 4) throw new BadRequestException("Password is too short (min 4 characters)");
        else if (body.getServerIp() != null && body.getServerIp().length() > 60) throw new BadRequestException("Server IP is too long (max 60 characters)");
        else if (body.getOwnerEmail() != null && body.getOwnerEmail().length() > 100) throw new BadRequestException("Owner email is too long (max 100 characters)");

        boolean isCaTokenValid = turnStileService.validate(body.getToken());
        if (!isCaTokenValid) throw new ResourceAccessForbiddenException();

        if (minecraftServerReportsService.existsByServerName(body.serverName)) {
            throw new BadRequestException("A key with this server name already exists.");
        }

        MinecraftServerReports server = new MinecraftServerReports();
        String newApiKey = Utils.generateApiKey(20);

        log.info("Generated new API key for server {}", body);

        server.setApiKey(newApiKey);
        server.setServerName(body.serverName);
        server.setPassword(body.getPassword());
        server.setServerIp(body.serverIp);
        try {
            server.setOwnerIp(body.ownerIp != null ? XorCodec.decodeFromBase64Url(body.ownerIp, serverInfo.getCryptoSecret()) : null);
        } catch (Exception ex) {
            log.warn(ex.getMessage());
            server.setOwnerIp(null);
        }
        server.setOwnerEmail(body.ownerEmail);
        server.setCreatedAt(LocalDateTime.now());
        server.setPaused(false);
        MinecraftServerReports saved = minecraftServerReportsService.save(server);

        auditLogService.saveLog(PortalLogType.TRANSCRIPT_API_KEY_CREATE, null, saved.getId().toString(), "API");
        MinecraftServerKeyDto response = new MinecraftServerKeyDto(saved.getServerName(), saved.getServerIp(), saved.getApiKey());

        return new ResponseEntity<>(new DefaultResponse(false, response, 1), HttpStatus.OK);
    }

    @GetMapping("/get/{uniqueId}")
    @RequiresMcApiKey
    @OptionalCookieAuth
    @PathLengthValidator
    public ResponseEntity<?> getTranscript(
            HttpServletRequest request,
            @PathVariable String uniqueId
    ) {
        DiscordReportTranscript reportTranscript = discordReportTranscriptService.findByUniqueId(uniqueId)
                .orElseThrow(ResourceNotFoundException::new);

        try {
            DiscordTranscript transcript = objectMapper.readValue(reportTranscript.getContent(), DiscordTranscript.class);
            return new ResponseEntity<>(new UIDResponse(false, uniqueId, transcript), HttpStatus.OK);
        } catch (Exception ex) {
            log.warn("Failed to parse transcript with id {}: {}", reportTranscript.getId(), ex.getMessage());
            throw new ResourceNotFoundException();
        }
    }

    @GetMapping("/getall")
    @RequiresMcApiKey
    public ResponseEntity<?> getMyTranscript(
            HttpServletRequest request
    ) {
        MinecraftServerReports uploader = (MinecraftServerReports) request.getAttribute("minecraftServerReport");

        List<DiscordReportTranscript> reportTranscriptList = discordReportTranscriptService.findAllByUserId(uploader.getId());

        List<DiscordTranscriptSummary> transcripts = reportTranscriptList.stream().map(t -> {
            String jsonText = t.getContent(); // map to DiscordTranscript
            try {
                DiscordTranscript trans = objectMapper.readValue(jsonText, DiscordTranscript.class);
                return new DiscordTranscriptSummary(
                        t.getUniqueId(),
                        trans.getChannelName(),
                        trans.getGeneratedAt(),
                        trans.getCreatedBy(),
                        trans.getTarget(),
                        trans.getCloseComment()
                );
            } catch (Exception ex) {
                log.warn("Failed to parse transcript with id {}: {}", t.getId(), ex.getMessage());
                return null;
            }
        }).filter(Objects::nonNull).toList();

        return new ResponseEntity<>(new DefaultResponse(false, transcripts, transcripts.size()), HttpStatus.OK);
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class NewMinecraftServerReportRequest {
        private String serverName;
        private String serverIp;
        private String password;
        private String ownerIp;
        private String ownerEmail;
        private String token;
    }

    public record MinecraftServerKeyDto(
            String serverName,
            String serverIp,
            String apiKey
    ) { }

    public static class TurnstileSiteverifyResponse {
        @JsonProperty("success")
        public Boolean success;

        @JsonProperty("challenge_ts")
        public String challengeTs;

        @JsonProperty("hostname")
        public String hostname;

        @JsonProperty("error-codes")
        public String[] errorCodes;
    }

    public record DiscordTranscriptSummary (
            String uniqueId,
            String channelName,
            String createdAt,
            String createdBy,
            String target,
            String closeComment
    ) { }
}
