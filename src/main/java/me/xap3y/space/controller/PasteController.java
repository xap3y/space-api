package me.xap3y.space.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import me.xap3y.space.api.enums.UserRole;
import me.xap3y.space.api.exception.BadRequestException;
import me.xap3y.space.api.exception.InvalidApiKeyException;
import me.xap3y.space.api.exception.ResourceNotFoundException;
import me.xap3y.space.api.iface.PathLengthValidator;
import me.xap3y.space.api.iface.RequiresApiKey;
import me.xap3y.space.config.ServerInfo;
import me.xap3y.space.dto.PasteDto;
import me.xap3y.space.entity.Paste;
import me.xap3y.space.entity.User;
import me.xap3y.space.mapper.PasteMapper;
import me.xap3y.space.model.request.PasteRequest;
import me.xap3y.space.model.response.DefaultResponse;
import me.xap3y.space.model.response.UIDResponse;
import me.xap3y.space.service.MetricService;
import me.xap3y.space.service.PasteService;
import me.xap3y.space.service.WebhookService;
import me.xap3y.space.util.ConfigDb;
import me.xap3y.space.util.Utils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Slf4j
@RestController
@RequestMapping("/v1/paste")
public class PasteController {


    private final PasteService pasteService;
    private final PasteMapper pasteMapper;
    private final ServerInfo serverInfo;
    private final MetricService metricService;
    private final WebhookService webhookService;

    //private static final String[] allowedExtensions = {"txt", "log", "java", "py", "sh", "json", "xml", "yml", "yaml", "properties", "md", "gradle", "conf", "cfg", "ini", "md", "markdown", "html", "htm", "css", "scss", "sass", "less", "ts", "js", "jsx", "tsx", "php", "sql", "csv", "tsv", "r", "rmd", "rdata", "rds", "rda", "rproj", "rhistory", "rprofile"};

    public PasteController(PasteService pasteService, PasteMapper pasteMapper, ServerInfo serverInfo, MetricService metricService, WebhookService webhookService) {
        this.pasteService = pasteService;
        this.pasteMapper = pasteMapper;
        this.serverInfo = serverInfo;
        this.metricService = metricService;
        this.webhookService = webhookService;
    }

    /*@PostMapping(
            value = "/create",
            consumes = {
                    MediaType.MULTIPART_FORM_DATA_VALUE
            },
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<?> createPaste(
            @RequestPart(value = "body", required = false) PasteRequest body,
            @RequestPart(value = "file", required = false) MultipartFile file,
            @RequestHeader("X-API-Key") String apiKey
    ) {
        if (body == null && file == null) {
            return new ResponseEntity<>(new JsonResponse(true, "Please provide either text or file, not both"), HttpStatus.BAD_REQUEST);
        } else if (file != null &&
                !file.isEmpty() &&
                //Arrays.stream(allowedExtensions).noneMatch(ext -> Objects.requireNonNull(file.getOriginalFilename()).endsWith("." + ext))
                //!Objects.requireNonNull(file.getContentType()).startsWith("text/")
                !Utils.containsText(file)
        ) {
            return new ResponseEntity<>(new JsonResponse(true, "Invalid file extension!"), HttpStatus.BAD_REQUEST);
        } else if (body != null && body.getText().length() > ConfigDb.MAX_PASTE_TEXT_LENGTH) {
            return new ResponseEntity<>(new JsonResponse(true, "Text is too long, max length is 55045 characters!"), HttpStatus.BAD_REQUEST);
        }

        String content;

        if (file != null) {
            try {
                content = new String(file.getBytes());
            } catch (Exception e) {
                log.error(e.getMessage());
                return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
            }
        } else {
            content = body.getText();
        }
        
        User uploader;
        try {
            uploader = apiKeyService.validateApiKey(apiKey);
        } catch (RuntimeException e) {
            if (e.getMessage().contains("Invalid API Key")) {
                return new ResponseEntity<>(new JsonResponse(true, "Invalid API Key!"), HttpStatus.UNAUTHORIZED);
            }
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }

        try {
            PasteDto savedPasteDto = pasteService.savePaste(content, uploader);
            String url2 = serverInfo.getBaseUrl() + "/v1/paste/get/" + savedPasteDto.uniqueId() + "?raw=true";
            return new ResponseEntity<>(new JsonResponse(false, savedPasteDto.uniqueId(), url2), HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }*/

    @PostMapping(
            value = "/create",
            consumes = {
                    MediaType.MULTIPART_FORM_DATA_VALUE
            },
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @RequiresApiKey
    public ResponseEntity<?> createPaste(
            HttpServletRequest request,
            @RequestParam(value = "file") MultipartFile file
    ) {

        User uploader = (User) request.getAttribute("uploader");
        if (uploader == null) return new ResponseEntity<>(new DefaultResponse(true, "Unauthorized"), HttpStatus.UNAUTHORIZED);

        if (file != null &&
                !file.isEmpty() &&
                //Arrays.stream(allowedExtensions).noneMatch(ext -> Objects.requireNonNull(file.getOriginalFilename()).endsWith("." + ext))
                //!Objects.requireNonNull(file.getContentType()).startsWith("text/")
                !Utils.containsText(file)
        ) {
            return new ResponseEntity<>(new DefaultResponse(true, "Invalid file extension!"), HttpStatus.BAD_REQUEST);
        } else if (file == null || file.isEmpty()) {
            return new ResponseEntity<>(new DefaultResponse(true, "Invalid file!"), HttpStatus.BAD_REQUEST);
        }

        String content;

        try {
            content = new String(file.getBytes());
        } catch (Exception e) {
            log.error(e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }

        try {
            PasteDto savedPasteDto = pasteService.savePaste(content, uploader);
            //log.info("GOT DTO");
            String url2 = serverInfo.getBaseUrl() + "/v1/paste/get/" + savedPasteDto.uniqueId() + "?raw=true";
            String webViewUrl = serverInfo.getBaseUrl() + "/web/paste-render/" + savedPasteDto.uniqueId();
            Map<String, String> additionalJson = new HashMap<>() {{
                put("message", url2);
                put("webview", webViewUrl);
            }};
            //log.info("RETURNING");
            metricService.setDatabaseUpdated(true);
            metricService.setSessionPastesCreated(metricService.getSessionPastesCreated() + 1);
            return new ResponseEntity<>(new UIDResponse(false, savedPasteDto.uniqueId(), additionalJson), HttpStatus.OK);
        } catch (Exception e) {
            log.error(e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping(
            value = "/create",
            consumes = {
                    MediaType.APPLICATION_JSON_VALUE
            },
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @RequiresApiKey
    public ResponseEntity<?> createPasteBody(
            HttpServletRequest request,
            @RequestBody PasteRequest body
    ) {
        User uploader = (User) request.getAttribute("uploader");
        if (uploader == null) {
            throw new InvalidApiKeyException();
        }
        if (body == null) {
            return new ResponseEntity<>(new DefaultResponse(true, "Please provide either text or file, not both"), HttpStatus.BAD_REQUEST);
        } else if (body.getText().length() > ConfigDb.MAX_PASTE_TEXT_LENGTH) {
            return new ResponseEntity<>(new DefaultResponse(true, "Text is too long, max length is 55045 characters!"), HttpStatus.BAD_REQUEST);
        }

        String title = body.getTitle();
        if (title == null || title.isEmpty()) {
            title = Utils.generateRandomId();
        }
        try {
            PasteDto savedPasteDto = pasteService.savePaste(title, body.getText(), uploader);
            metricService.setDatabaseUpdated(true);
            metricService.setSessionPastesCreated(metricService.getSessionPastesCreated() + 1);
            webhookService.postPasteCreated(savedPasteDto);
            return new ResponseEntity<>(new UIDResponse(false, savedPasteDto.uniqueId(), savedPasteDto), HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @DeleteMapping(
            value = "/get/{uniqueId}"
    )
    @RequiresApiKey
    @PathLengthValidator
    public ResponseEntity<?> deletePaste(
            HttpServletRequest request,
            @PathVariable String uniqueId
    ) {
        User uploader = (User) request.getAttribute("uploader");
        if (uploader == null) throw new InvalidApiKeyException();

        Paste paste = pasteService.getPasteByUniqueId(uniqueId).orElseThrow(() -> new ResourceNotFoundException("Paste not found"));

        if (!Objects.equals(paste.getCreatedBy().getId(), uploader.getId())  &&
                (uploader.getRole() == UserRole.USER
                        || uploader.getRole() == UserRole.GUEST
                        || uploader.getRole() == UserRole.TESTER
                )) {
            throw new InvalidApiKeyException();
        }

        pasteService.deleteByUniqueId(paste.getUniqueId());
        metricService.setDatabaseUpdated(true);
        //metricService.setSessionPastesCreated(metricService.getSessionPastesCreated() - 1);
        return new ResponseEntity<>(new DefaultResponse(false, "Paste deleted"), HttpStatus.OK);
    }

    @GetMapping(
            value = "/get/{uniqueId}",
            produces = {
                    MediaType.APPLICATION_JSON_VALUE,
                    MediaType.TEXT_PLAIN_VALUE
            }
    )
    @PathLengthValidator
    public ResponseEntity<?> getPaste(
            @PathVariable String uniqueId,
            @RequestParam(required = false, defaultValue = "false", value = "raw") boolean rawData
    ) {
        PasteDto pasteDto = pasteService.getPasteByUniqueId(uniqueId)
                .map(pasteMapper)
                .orElseThrow(() -> new ResourceNotFoundException("Paste not found"));

        HttpHeaders headers = new HttpHeaders();

        headers.add("X-Paste-IsPublic", String.valueOf(pasteDto.isPublic()));

        if (rawData) {
            headers.set(HttpHeaders.CONTENT_TYPE, "text/plain;charset=UTF-8");
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(pasteDto.content());
        } else {
            headers.set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(new UIDResponse(false, pasteDto.uniqueId(),pasteDto));
        }
    }

    @GetMapping(
            value = "/stats",
            produces = {
                    MediaType.APPLICATION_JSON_VALUE
            }
    )
    public ResponseEntity<?> getPaste() {
        LocalDate date = LocalDate.now().minusYears(1);
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = LocalDateTime.now();

        Map<String, ?> stats = pasteService.getStats(startOfDay, endOfDay);

        if (stats == null) {
            return new ResponseEntity<>(new DefaultResponse(true, "No pastes found"), HttpStatus.NOT_FOUND);
        }

        return new ResponseEntity<>(new DefaultResponse(false, stats), HttpStatus.OK);
    }
}
