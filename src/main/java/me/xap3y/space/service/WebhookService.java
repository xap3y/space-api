package me.xap3y.space.service;

import lombok.extern.slf4j.Slf4j;
import me.xap3y.space.SpaceApplication;
import me.xap3y.space.config.ServerInfo;
import me.xap3y.space.dto.NewImageDto;
import me.xap3y.space.dto.PasteDto;
import me.xap3y.space.dto.UrlDto;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.io.IOException;
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
        postMessage("Space-API started at " + startedAt.toString() + " running on " + serverInfo.getBaseUrl() + " ENV:**" + SpaceApplication.env + "** | VER:**" + SpaceApplication.VERSION + "**");
    }

    private DiscordWebhook getHook() {
        DiscordWebhook hook = new DiscordWebhook(serverInfo.getDiscordBotToken());
        hook.setUsername("Space-API");
        return hook;
    }

    public void postMessage(String message) {
        DiscordWebhook hook = getHook();
        hook.setContent(message);
        try {
            hook.execute();
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    public void postImageUpload(String id, NewImageDto imageDto) {
        DiscordWebhook hook = getHook();
        DiscordWebhook.EmbedObject embedObject = new DiscordWebhook.EmbedObject();
        embedObject.setColor(Color.GREEN);
        embedObject.setTitle("Image Uploaded! (" + id + "." + imageDto.type().toLowerCase(Locale.ROOT) + ")");
        embedObject.setThumbnail(serverInfo.getBaseUrl() + "/v1/image/get/" + id);
        embedObject.setUrl(serverInfo.getBaseUrl() + "/v1/image/get/" + id);
        embedObject.addField("UID", id, true);
        //embedObject.addField("URL", serverInfo.getBaseUrl() + "/v1/image/get/" + id, true);
        embedObject.addField("| SIZE", "**|** " + imageDto.size() / 1024 + " KiB", true);
        embedObject.addField("| UPLOADER", "**|** [" + imageDto.uploader().getUsername() + "](https://xap3y.space/profile/xap3y)" , true);
        //embedObject.setDescription("SIZE: `" + imageDto.size() + "` UPLOADER: " + imageDto.uploader().getUsername() + "(" + imageDto.uploader().getId() + ")");
        hook.addEmbed(embedObject);
        try {
            hook.execute();
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    public void postUrlShorten(UrlDto urlDto) {
        DiscordWebhook hook = getHook();
        DiscordWebhook.EmbedObject embedObject = new DiscordWebhook.EmbedObject();
        embedObject.setColor(Color.ORANGE);
        embedObject.setTitle("URL Shortened! (" + urlDto.shortCode() + ")");
        embedObject.setUrl(urlDto.url());
        embedObject.addField("shortURL", "https://r0.xap3y.tech/" + urlDto.shortCode(), false);
        embedObject.addField("creator", urlDto.uploader(), false);
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
        DiscordWebhook.EmbedObject embedObject = new DiscordWebhook.EmbedObject();
        embedObject.setColor(Color.ORANGE);
        embedObject.setTitle("Paste Created! (" + pasteDto.uniqueId() + ")");
        embedObject.setUrl(serverInfo.getBaseUrl() + "/v1/paste/get/" + pasteDto.uniqueId());
        embedObject.addField("shortURL", "https://p0.xap3y.tech/" + pasteDto.uniqueId(), false);
        embedObject.addField("creator", pasteDto.uploader(), false);
        //embedObject.setDescription("SIZE: `" + imageDto.size() + "` UPLOADER: " + imageDto.uploader().getUsername() + "(" + imageDto.uploader().getId() + ")");
        hook.addEmbed(embedObject);
        try {
            hook.execute();
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }
}
