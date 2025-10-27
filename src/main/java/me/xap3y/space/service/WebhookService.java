package me.xap3y.space.service;

import lombok.extern.slf4j.Slf4j;
import me.xap3y.space.SpaceApplication;
import me.xap3y.space.config.ServerInfo;
import me.xap3y.space.dto.*;
import me.xap3y.space.entity.Image;
import me.xap3y.space.entity.Session;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import static me.xap3y.space.SpaceApplication.startedAt;

@Slf4j
@Service
public class WebhookService {

    private final ServerInfo serverInfo;

    public WebhookService(ServerInfo serverInfo) {
        this.serverInfo = serverInfo;
    }

    public void init() {
        postMessage("Space-API started at " + startedAt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + " running on " + serverInfo.getBaseUrl() + " ENV:**" + SpaceApplication.env + "** | VER:**" + SpaceApplication.VERSION + "**");
    }

    private DiscordWebhook getHook() {
        if (!serverInfo.getUseDiscordWebhook()) return null;
        DiscordWebhook hook = new DiscordWebhook(serverInfo.getDiscordBotToken());
        hook.setUsername("Space-API");
        return hook;
    }

    public void postMessage(String message) {
        if (!serverInfo.getUseDiscordWebhook()) return;
        DiscordWebhook hook = getHook();
        hook.setContent(message);
        try {
            hook.execute();
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    public void postImageUpload(String id, ImageInfoDto imageDto) {
        DiscordWebhook hook = getHook();
        if (hook == null) return;
        DiscordWebhook.EmbedObject embedObject = new DiscordWebhook.EmbedObject();
        embedObject.setColor(Color.GREEN);
        embedObject.setTitle("Image Uploaded! (" + id + "." + imageDto.type().toLowerCase(Locale.ROOT) + ")");
        embedObject.setThumbnail(serverInfo.getBaseUrl() + "/v1/image/get/" + id);
        embedObject.setUrl(serverInfo.getBaseUrl() + "/v1/image/get/" + id);
        embedObject.addField("UID", id, true);
        //embedObject.addField("URL", serverInfo.getBaseUrl() + "/v1/image/get/" + id, true);
        embedObject.addField("| SIZE", "**|** " + imageDto.size() / 1024 + " KiB", true);
        embedObject.addField("| UPLOADER", "**|** [" + imageDto.uploader().username() + "](https://s.xap3y.tech/profile/xap3y)" , true);
        //embedObject.setDescription("SIZE: `" + imageDto.size() + "` UPLOADER: " + imageDto.uploader().getUsername() + "(" + imageDto.uploader().getId() + ")");
        hook.addEmbed(embedObject);
        hook.setContent(
                "[RAW_URL](" + serverInfo.getBaseUrl() + "/v1/image/get/" + id + ")" +
                        " | [WEB_URL](" + serverInfo.getBaseUrl() + "/web/image-render/" + id + ")" +
                        " | [PORTAL_URL](" + serverInfo.getFrontEndUrl() + "/image/" + id + ")" +
                        " | [SHORT_URL](" + serverInfo.getShortImageUrl() + "/" + id + ")");
        try {
            hook.execute();
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    public void postUrlShorten(ShortUrlDto urlDto) {
        DiscordWebhook hook = getHook();
        if (hook == null) return;
        DiscordWebhook.EmbedObject embedObject = new DiscordWebhook.EmbedObject();
        embedObject.setColor(Color.ORANGE);
        embedObject.setTitle("URL Shortened! (" + urlDto.uniqueId() + ")");
        embedObject.setUrl(urlDto.urlSet().rawUrl());
        embedObject.addField("shortURL", "https://r0.xap3y.tech/" + urlDto.uniqueId(), false);
        embedObject.addField("creator", urlDto.uploader().username(), false);
        //embedObject.setDescription("SIZE: `" + imageDto.size() + "` UPLOADER: " + imageDto.uploader().getUsername() + "(" + imageDto.uploader().getId() + ")");
        hook.addEmbed(embedObject);
        try {
            hook.execute();
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    public void postPasteCreated(PasteDto pasteDto) {
        DiscordWebhook hook = getHook();
        if (hook == null) return;
        DiscordWebhook.EmbedObject embedObject = new DiscordWebhook.EmbedObject();
        embedObject.setColor(Color.ORANGE);
        embedObject.setTitle("Paste Created! (" + pasteDto.uniqueId() + ")");
        embedObject.setUrl(serverInfo.getBaseUrl() + "/v1/paste/get/" + pasteDto.uniqueId());
        embedObject.addField("shortURL", "https://p0.xap3y.tech/" + pasteDto.uniqueId(), false);
        embedObject.addField("creator", pasteDto.uploader().username(), false);
        //embedObject.setDescription("SIZE: `" + imageDto.size() + "` UPLOADER: " + imageDto.uploader().getUsername() + "(" + imageDto.uploader().getId() + ")");
        hook.addEmbed(embedObject);
        try {
            hook.execute();
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    public void postImageDeleted(String uniqueId, Image img) {
        DiscordWebhook hook = getHook();
        if (hook == null) return;
        DiscordWebhook.EmbedObject embedObject = new DiscordWebhook.EmbedObject();
        embedObject.setColor(Color.RED);
        embedObject.setTitle("Image Deleted! (" + uniqueId + ")");
        embedObject.addField("| SIZE", "**|** " + img.getSize() / 1024 + " KiB", true);
        embedObject.addField("| UPLOADER", "**|** [" + img.getUploader().getUsername() + "](" + serverInfo.getFrontEndUrl() + "/user/" + img.getUploader().getUsername() + ")" , true);
        hook.addEmbed(embedObject);
        try {
            hook.execute();
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    public void postSessionInit(Session session) {
        postMessage("> [SESSION-INIT] U: [" + session.getUser().getUsername() + "](" + serverInfo.getFrontEndUrl() + "/user/" + session.getUser().getUsername() + ") F: **" + session.getIpAddress() + "** A: **" + session.getUserAgent() + "**");
    }

    public void postSessionInvalid(Session session) {
        postMessage("> [SESSION-INVALIDATED] U: [" + session.getUser().getUsername() + "](" + serverInfo.getFrontEndUrl() + "/user/" + session.getUser().getUsername() + ") -ID: **" + session.getId() + "**  -F: **" + session.getIpAddress() + "** -A: **" + session.getUserAgent() + "**");
    }
}
