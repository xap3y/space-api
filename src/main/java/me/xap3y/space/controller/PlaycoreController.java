package me.xap3y.space.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import me.xap3y.space.api.iface.OptionalApiKey;
import me.xap3y.space.handler.PlaycoreWebInSocketHandler;
import me.xap3y.space.model.pcv.*;
import me.xap3y.space.model.response.DefaultResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@Slf4j
@RestController
@RequestMapping("/v1/pcv")
public class PlaycoreController {

    private final PlaycoreWebInSocketHandler playcoreWebInSocketHandler;

    public PlaycoreController(PlaycoreWebInSocketHandler playcoreWebInSocketHandler) {
        this.playcoreWebInSocketHandler = playcoreWebInSocketHandler;
    }

    @GetMapping(
            value = "/data/{uniqueId}",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @OptionalApiKey
    public ResponseEntity<?> openConnection(
            @PathVariable String uniqueId,
            HttpServletRequest request
    ) {

        if (!playcoreWebInSocketHandler.isConnected(uniqueId)) {
            return new ResponseEntity<>(new DefaultResponse(true, "WS is not running"), HttpStatus.NOT_FOUND);
        }

        PlaycoreStorageModel storage = playcoreWebInSocketHandler.storage.get(uniqueId);

        return new ResponseEntity<>(new DefaultResponse(true, storage), HttpStatus.OK);
    }

    @GetMapping(
            value = "/status/{uniqueId}",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @OptionalApiKey
    public ResponseEntity<?> statusCheck(
            @PathVariable String uniqueId,
            HttpServletRequest request
    ) {
        if (!playcoreWebInSocketHandler.isConnected(uniqueId)) {
            return new ResponseEntity<>(new DefaultResponse(true, "WS is not running"), HttpStatus.NOT_FOUND);
        }

        return new ResponseEntity<>(new DefaultResponse(true, "OK"), HttpStatus.NO_CONTENT);
    }

    @PostMapping(
            value = "/data/{uniqueId}/vip",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @OptionalApiKey
    public ResponseEntity<?> postVipUpdate(
            @PathVariable String uniqueId,
            @RequestBody VipPackage body,
            HttpServletRequest request
    ) {
        ResponseEntity<?> connectionCheck = requireConnectedNoLog(uniqueId);
        if (connectionCheck != null) return connectionCheck;

        try {
            playcoreWebInSocketHandler.sendJsonToSession(uniqueId, new OutWebSocketMessage(
                    "VIP_MODIFY",
                    body
            ));
        } catch (IOException e) {
            log.error("Error sending VIP_MODIFY message to session {}: {}", uniqueId, e.getMessage());
            return new ResponseEntity<>(new DefaultResponse(false, "Failed to send VIP_MODIFY message"), HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return new ResponseEntity<>(new DefaultResponse(true, "BROADCASTED"), HttpStatus.OK);
    }

    @PostMapping(
            value = "/data/{uniqueId}/code",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @OptionalApiKey
    public ResponseEntity<?> postCodeUpdate(
            @PathVariable String uniqueId,
            @RequestBody PlaycoreCode body,
            HttpServletRequest request
    ) {
        ResponseEntity<?> connectionCheck = requireConnectedNoLog(uniqueId);
        if (connectionCheck != null) return connectionCheck;

        try {
            playcoreWebInSocketHandler.sendJsonToSession(uniqueId, new OutWebSocketMessage(
                    "CODE_MODIFY",
                    body
            ));
        } catch (IOException e) {
            log.error("Error sending CODE_MODIFY message to session {}: {}", uniqueId, e.getMessage());
            return new ResponseEntity<>(new DefaultResponse(false, "Failed to send CODE_MODIFY message"), HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return new ResponseEntity<>(new DefaultResponse(true, "BROADCASTED"), HttpStatus.OK);
    }

    @PostMapping(
            value = "/data/{uniqueId}/activevip",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @OptionalApiKey
    public ResponseEntity<?> postActiveVipUpdate(
            @PathVariable String uniqueId,
            @RequestBody ActiveVipModifyRequest body,
            HttpServletRequest request
    ) {
        if (!playcoreWebInSocketHandler.isConnected(uniqueId)) {
            return new ResponseEntity<>(new DefaultResponse(true, "WS is not running"), HttpStatus.NOT_FOUND);
        }

        try {
            playcoreWebInSocketHandler.sendJsonToSession(uniqueId, new OutWebSocketMessage(
                    "ACTIVE_MODIFY",
                    body
            ));
        } catch (IOException e) {
            log.error("Error sending ACTIVE_MODIFY message to session {}: {}", uniqueId, e.getMessage());
            return new ResponseEntity<>(new DefaultResponse(false, "Failed to send ACTIVE_MODIFY message"), HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return new ResponseEntity<>(new DefaultResponse(true, "BROADCASTED"), HttpStatus.OK);
    }

    @PostMapping(value = "/data/{uniqueId}/pausedvip", produces = MediaType.APPLICATION_JSON_VALUE)
    @OptionalApiKey
    public ResponseEntity<?> postPausedVipUpdate(@PathVariable String uniqueId, @RequestBody PausedVipModifyRequest body,
                                                  HttpServletRequest request) {
        ResponseEntity<?> connectionCheck = requireConnectedNoLog(uniqueId);
        if (connectionCheck != null) return connectionCheck;
        try {
            playcoreWebInSocketHandler.sendJsonToSession(uniqueId, new OutWebSocketMessage("PAUSED_MODIFY", body));
        } catch (IOException e) {
            return new ResponseEntity<>(new DefaultResponse(false, "Failed to send PAUSED_MODIFY message"), HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return new ResponseEntity<>(new DefaultResponse(true, "BROADCASTED"), HttpStatus.OK);
    }


    @DeleteMapping(
            value = "/data/{uniqueId}/code/{code}",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @OptionalApiKey
    public ResponseEntity<?> deleteCode(
            @PathVariable String uniqueId,
            @PathVariable String code,
            HttpServletRequest request
    ) {
        ResponseEntity<?> connectionCheck = requireConnectedNoLog(uniqueId);
        if (connectionCheck != null) return connectionCheck;

        playcoreWebInSocketHandler.postDeleteResource(uniqueId, new ResourceDelete(code, "CODE"));

        return new ResponseEntity<>(new DefaultResponse(true, "DELETED"), HttpStatus.OK);
    }

    @DeleteMapping(
            value = "/data/{uniqueId}/active_vip/{uuid}",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @OptionalApiKey
    public ResponseEntity<?> deleteActiveVip(
            @PathVariable String uniqueId,
            @PathVariable String uuid,
            HttpServletRequest request
    ) {
        ResponseEntity<?> connectionCheck = requireConnectedNoLog(uniqueId);
        if (connectionCheck != null) return connectionCheck;

        playcoreWebInSocketHandler.postDeleteResource(uniqueId, new ResourceDelete(uuid, "ACTIVE_VIP"));

        return new ResponseEntity<>(new DefaultResponse(true, "DELETED"), HttpStatus.OK);
    }

    @DeleteMapping(value = "/data/{uniqueId}/paused_vip/{uuid}/{packageName}", produces = MediaType.APPLICATION_JSON_VALUE)
    @OptionalApiKey
    public ResponseEntity<?> deletePausedVip(@PathVariable String uniqueId, @PathVariable String uuid,
                                             @PathVariable String packageName, HttpServletRequest request) {
        ResponseEntity<?> connectionCheck = requireConnectedNoLog(uniqueId);
        if (connectionCheck != null) return connectionCheck;
        playcoreWebInSocketHandler.postDeleteResource(uniqueId, new ResourceDelete(uuid + ":" + packageName, "PAUSED_VIP"));
        return new ResponseEntity<>(new DefaultResponse(true, "DELETED"), HttpStatus.OK);
    }

    @PostMapping(
            value = "/scrape/{uniqueId}",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @OptionalApiKey
    public ResponseEntity<?> scrapeData(
            @PathVariable String uniqueId,
            HttpServletRequest request
    ) {

        ResponseEntity<?> connectionCheck = requireConnectedNoLog(uniqueId);
        if (connectionCheck != null) return connectionCheck;

        playcoreWebInSocketHandler.sendMessageToSession(uniqueId, "SCRAPE");

        return new ResponseEntity<>(new DefaultResponse(true, "OK"), HttpStatus.NO_CONTENT);
    }

    @PostMapping(
            value = "/clear/{uniqueId}",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @OptionalApiKey
    public ResponseEntity<?> clearData(
            @PathVariable String uniqueId,
            HttpServletRequest request
    ) {

        ResponseEntity<?> connectionCheck = requireConnectedNoLog(uniqueId);
        if (connectionCheck != null) return connectionCheck;

        playcoreWebInSocketHandler.clearDataForSession(uniqueId);

        return new ResponseEntity<>(new DefaultResponse(true, "OK"), HttpStatus.NO_CONTENT);
    }

    @PostMapping(
            value = "/scrape/{uniqueId}/codes",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @OptionalApiKey
    public ResponseEntity<?> getCodes(
            @PathVariable String uniqueId,
            HttpServletRequest request
    ) {

        ResponseEntity<?> connectionCheck = requireConnectedNoLog(uniqueId);
        if (connectionCheck != null) return connectionCheck;

        playcoreWebInSocketHandler.sendMessageToSession(uniqueId, "SCRAPE_CODES");

        return new ResponseEntity<>(new DefaultResponse(true, "OK"), HttpStatus.NO_CONTENT);
    }

    @PostMapping(
            value = "/scrape/{uniqueId}/vips",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @OptionalApiKey
    public ResponseEntity<?> getPackages(
            @PathVariable String uniqueId,
            HttpServletRequest request
    ) {

        ResponseEntity<?> connectionCheck = requireConnectedNoLog(uniqueId);
        if (connectionCheck != null) return connectionCheck;

        playcoreWebInSocketHandler.sendMessageToSession(uniqueId, "SCRAPE_VIP");

        return new ResponseEntity<>(new DefaultResponse(true, "OK"), HttpStatus.NO_CONTENT);
    }

    @PostMapping(
            value = "/scrape/{uniqueId}/activevips",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @OptionalApiKey
    public ResponseEntity<?> getActiveVips(
            @PathVariable String uniqueId,
            HttpServletRequest request
    ) {

        ResponseEntity<?> connectionCheck = requireConnectedNoLog(uniqueId);
        if (connectionCheck != null) return connectionCheck;

        playcoreWebInSocketHandler.sendMessageToSession(uniqueId, "SCRAPE_ACTIVE_VIP");

        return new ResponseEntity<>(new DefaultResponse(true, "OK"), HttpStatus.NO_CONTENT);
    }

    @PostMapping(value = "/scrape/{uniqueId}/pausedvips", produces = MediaType.APPLICATION_JSON_VALUE)
    @OptionalApiKey
    public ResponseEntity<?> getPausedVips(@PathVariable String uniqueId, HttpServletRequest request) {
        ResponseEntity<?> connectionCheck = requireConnectedNoLog(uniqueId);
        if (connectionCheck != null) return connectionCheck;
        playcoreWebInSocketHandler.sendMessageToSession(uniqueId, "SCRAPE_PAUSED_VIP");
        return new ResponseEntity<>(new DefaultResponse(true, "OK"), HttpStatus.NO_CONTENT);
    }

    @PostMapping(value = "/scrape/{uniqueId}/groups", produces = MediaType.APPLICATION_JSON_VALUE)
    @OptionalApiKey
    public ResponseEntity<?> getGroups(@PathVariable String uniqueId, HttpServletRequest request) {
        ResponseEntity<?> connectionCheck = requireConnectedNoLog(uniqueId);
        if (connectionCheck != null) return connectionCheck;
        playcoreWebInSocketHandler.sendMessageToSession(uniqueId, "SCRAPE_GROUPS");
        return new ResponseEntity<>(new DefaultResponse(true, "OK"), HttpStatus.NO_CONTENT);
    }

    private ResponseEntity<?> requireConnectedNoLog(String uniqueId) {
        if (!playcoreWebInSocketHandler.isConnected(uniqueId)) {
            return new ResponseEntity<>(new DefaultResponse(true, "WS is not running"), HttpStatus.NOT_FOUND);
        }
        return null;
    }
}
