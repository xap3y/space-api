package me.xap3y.space.discord.commands;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.Color;
import lombok.SneakyThrows;
import me.xap3y.space.discord.Emoji;
import me.xap3y.space.dto.ImageInfoDto;
import me.xap3y.space.entity.DiscordConnection;
import me.xap3y.space.entity.Image;
import me.xap3y.space.mapper.ImageMapper;
import me.xap3y.space.service.DiscordConnectionService;
import me.xap3y.space.service.ImageService;
import me.xap3y.space.util.Utils;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Component
public class ImageCommand implements SlashCommand{

    private final ImageService imageService;
    private final Utils utils;
    private final DiscordConnectionService discordConnectionService;
    private final ImageMapper imageMapper;

    public ImageCommand(ImageService imageService, Utils utils, DiscordConnectionService discordConnectionService, ImageMapper imageMapper) {
        this.imageService = imageService;
        this.utils = utils;
        this.discordConnectionService = discordConnectionService;
        this.imageMapper = imageMapper;
    }

    @Override
    public String getName() {
        return "image";
    }

    @Override
    @SneakyThrows
    public Mono<Void> handle(ChatInputInteractionEvent event) {

        String uniqueId = event.getOption("uid")
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::asString)
                .orElseThrow(() -> new IllegalArgumentException("No UID provided"));

        Image image;
        try {
            image = imageService.getImage(uniqueId);
        } catch (Exception e) {
            return event.reply()
                    .withEphemeral(false)
                    .withContent(Emoji.ERROR.getUnicode() + " Image with UID `" + uniqueId + "` not found!")
                    .then();
        }

        ImageInfoDto imageInfoDto = imageMapper.apply(image);

        if (imageInfoDto == null) {
            return event.reply()
                    .withEphemeral(false)
                    .withContent("Image with UID `" + uniqueId + "` not found!")
                    .then();
        } else if (!imageInfoDto.isPublic() || imageInfoDto.requiresPassword()) {
            return event.reply()
                    .withEphemeral(false)
                    .withContent("Image with UID `" + uniqueId + "` is private and requires a password to view!")
                    .then();
        }

        String uploaderDiscord = "";

        Optional<DiscordConnection> discordConnection = discordConnectionService.findByUserId(image.getUploader());

        if (discordConnection.isPresent()) {
            uploaderDiscord = " (" + Utils.structDiscordUserTag(discordConnection.get().getDiscordId()) + ")";
        }

        EmbedCreateSpec embed = EmbedCreateSpec.builder()
                .color(Color.YELLOW)
                .title(uniqueId + "." + image.getFileType() + " (" + imageInfoDto.size() + " KB)")
                /*.url(imageInfoDto.urlSet().portalUrl())*/
                .image(imageInfoDto.urlSet().rawUrl()) // TODO
                .addField("Uploaded by  ", utils.structDiscordProfileLink(imageInfoDto.uploader().username())  + uploaderDiscord, true)
                .addField("Uploaded at", imageInfoDto.uploadedAt().format(DateTimeFormatter.ofPattern("yyyy/MM/d HH:mm:ss")), true)
                /*.addField("\u200b", "\u200b", true)*/
                .timestamp(Instant.now())
                .build();

        Button viewButton = Button.link(imageInfoDto.urlSet().portalUrl(), "View");
        Button downloadButton = Button.link(imageInfoDto.urlSet().rawUrl() + "?download=true", "Download");

        return event.reply()
                .withEphemeral(false)
                .withEmbeds(embed)
                .withComponents(ActionRow.of(viewButton, downloadButton))
                .then();
    }
}
