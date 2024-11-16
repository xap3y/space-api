package me.xap3y.space.controller;

import lombok.extern.slf4j.Slf4j;
import me.xap3y.space.config.ServerInfo;
import me.xap3y.space.dto.JsonResponse;
import me.xap3y.space.dto.PasteDto;
import me.xap3y.space.entity.User;
import me.xap3y.space.exception.ResourceNotFoundException;
import me.xap3y.space.mapper.PasteMapper;
import me.xap3y.space.model.PasteRequest;
import me.xap3y.space.service.ApiKeyService;
import me.xap3y.space.service.PasteService;
import me.xap3y.space.util.ConfigDb;
import me.xap3y.space.util.Utils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequestMapping("/v1/paste")
public class PasteController {


    private final PasteService pasteService;
    private final ApiKeyService apiKeyService;
    private final PasteMapper pasteMapper;
    private final ServerInfo serverInfo;

    //private static final String[] allowedExtensions = {"txt", "log", "java", "py", "sh", "json", "xml", "yml", "yaml", "properties", "md", "gradle", "conf", "cfg", "ini", "md", "markdown", "html", "htm", "css", "scss", "sass", "less", "ts", "js", "jsx", "tsx", "php", "sql", "csv", "tsv", "r", "rmd", "rdata", "rds", "rda", "rproj", "rhistory", "rprofile"};

    public PasteController(PasteService pasteService, ApiKeyService apiKeyService, PasteMapper pasteMapper, ServerInfo serverInfo) {
        this.pasteService = pasteService;
        this.apiKeyService = apiKeyService;
        this.pasteMapper = pasteMapper;
        this.serverInfo = serverInfo;
    }

    @PostMapping(
            value = "/create",
            consumes = {
                    MediaType.MULTIPART_FORM_DATA_VALUE
            },
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<JsonResponse> createPaste(
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
            String url2 = serverInfo.getProtocol() + "://" + serverInfo.getHost() + ":" + serverInfo.getPort() + "/v1/paste/get/" + savedPasteDto.uniqueId() + "?raw=true";
            return new ResponseEntity<>(new JsonResponse(false, savedPasteDto.uniqueId(), url2), HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping(
            value = "/get/{uniqueId}",
            produces = {
                    MediaType.APPLICATION_JSON_VALUE,
                    MediaType.TEXT_PLAIN_VALUE
            }
    )
    public ResponseEntity<Object> getPaste(
            @PathVariable String uniqueId,
            @RequestParam(required = false, defaultValue = "false", value = "raw") boolean rawData,
            @RequestParam(required = false, defaultValue = "false", value = "uploader_info") boolean getUserInfo,
            @RequestParam(required = false, defaultValue = "false", value = "paste_info") boolean pasteInfo
    ) {

        PasteDto pasteDto = pasteService.getPasteByUniqueId(uniqueId)
                .map(pasteMapper)
                .orElseThrow(() -> new ResourceNotFoundException("Paste not found"));

        HttpHeaders headers = new HttpHeaders();

        if (getUserInfo) {
            headers.add("X-Uploader", pasteDto.uploader());
        }
        if (pasteInfo) {
            headers.add("X-Paste-CreatedAt", pasteDto.createdAt().toString());
        }

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
                    .body(new JsonResponse(false, pasteDto.uniqueId(),pasteDto.content()));
        }
    }
}
