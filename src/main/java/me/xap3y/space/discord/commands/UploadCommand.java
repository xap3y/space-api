package me.xap3y.space.discord.commands;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.object.entity.Attachment;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.Color;
import me.xap3y.space.api.enums.ArchiveType;
import me.xap3y.space.api.enums.ResourceSourceType;
import me.xap3y.space.config.ServerInfo;
import me.xap3y.space.discord.Emoji;
import me.xap3y.space.dto.FoundImageDto;
import me.xap3y.space.dto.ImageInfoDto;
import me.xap3y.space.entity.DiscordConnection;
import me.xap3y.space.entity.Image;
import me.xap3y.space.entity.User;
import me.xap3y.space.mapper.ImageMapper;
import me.xap3y.space.service.DiscordConnectionService;
import me.xap3y.space.service.ImageService;
import me.xap3y.space.util.Utils;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Component
public class UploadCommand implements SlashCommand{

    private final DiscordConnectionService discordConnectionService;
    private final ImageService imageService;
    private final ServerInfo serverInfo;
    private final ImageMapper imageMapper;

    public UploadCommand(DiscordConnectionService discordConnectionService, ImageService imageService, ServerInfo serverInfo, ImageMapper imageMapper) {
        this.discordConnectionService = discordConnectionService;
        this.imageService = imageService;
        this.serverInfo = serverInfo;
        this.imageMapper = imageMapper;
    }

    @Override
    public String getName() {
        return "upload";
    }

    @Override
    public Mono<Void> handle(ChatInputInteractionEvent event) {

        Attachment image = event.getOption("image")
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::asAttachment)
                .orElseThrow(() -> new IllegalArgumentException("No image provided"));

        String urlStr = image.getUrl();
        String fileName = image.getFilename();

        ArchiveType archiveType = ArchiveType.getExtensionType(fileName);

        if (archiveType != null) {
            List<FoundImageDto> foundImages;

            MultipartFile multipartFile = Utils.createMultipartFileFromUrl(urlStr, fileName);
            try {
                foundImages = Utils.extractFoundImages(multipartFile, archiveType);
            } catch (Exception e) {
                return event.reply()
                        .withEphemeral(false)
                        .withContent(Emoji.ERROR.getUnicode() + e.getMessage())
                        .then();
            }

            if (foundImages.isEmpty()) {
                return event.reply()
                        .withEphemeral(false)
                        .withContent( Emoji.ERROR.getUnicode() + "No images found in the " + archiveType.name() + " file.")
                        .then();
            }

            StringBuilder response = new StringBuilder(Emoji.CHECK.getUnicode() + " Found " + foundImages.size() + " images in the " + archiveType.name() + " file:\n");
            for (FoundImageDto foundImage : foundImages) {
                response.append("> ").append(foundImage.getOriginalName()).append(" - ").append(foundImage.getMimeType()).append(" - ").append(foundImage.getSize()).append("Kb").append("\n");
            }

            return event.reply()
                    .withEphemeral(false)
                    .withContent(response.toString())
                    .then();
        }

        // check if the attachment is an image
        if (!image.getContentType().orElse("").startsWith("image/")) {
            return event.reply()
                    .withEphemeral(true)
                    .withContent("Please upload a valid image file.")
                    .then();
        } else if (image.getSize() > 50 * 1024 * 1024) {
            return event.reply()
                    .withEphemeral(true)
                    .withContent("Image size exceeds the 50MB limit.")
                    .then();
        }

        String userId = event.getInteraction().getUser().getId().asString();

        Optional<DiscordConnection> discordConnection = discordConnectionService.findByDiscordId(userId);

        if (discordConnection.isEmpty()) {
            return event.reply()
                    .withEphemeral(false)
                    .withContent( Emoji.ERROR.getUnicode() + " You need to link your Discord account to upload images.")
                    .then();
        }

        User user = discordConnection.get().getUserId();

        MultipartFile multipartFile = Utils.createMultipartFileFromUrl(urlStr, fileName);

        if (multipartFile == null) {
            return event.reply()
                    .withEphemeral(true)
                    .withContent("Failed to process the image. Please try again.")
                    .then();
        }

        try {
            Image savedImage = imageService.saveImage(multipartFile, user, null, null, true, null, ResourceSourceType.DISCORD);

            ImageInfoDto imageInfoDto = imageMapper.apply(savedImage);

            Button viewButton = Button.link(imageInfoDto.urlSet().portalUrl(), "View");
            Button downloadButton = Button.link(imageInfoDto.urlSet().rawUrl() + "?download=true", "Download");

            EmbedCreateSpec embed = EmbedCreateSpec.builder()
                    .color(Color.LIGHT_SEA_GREEN)
                    .description(Emoji.CHECK.getUnicode() + " Image uploaded successfully: " + Utils.structDiscordLink(imageInfoDto.uniqueId() + "." + imageInfoDto.type(), serverInfo.getFrontEndUrl() + "/i/" + imageInfoDto.uniqueId()))
                    .build();

            return event.reply()
                    .withEphemeral(false)
                    .withEmbeds(embed)
                    .withComponents(ActionRow.of(viewButton, downloadButton))
                    .then();
        } catch (IOException e) {
            return event.reply()
                    .withEphemeral(true)
                    .withContent("Failed to save the image. Please try again.")
                    .then();
        }
    }
}
