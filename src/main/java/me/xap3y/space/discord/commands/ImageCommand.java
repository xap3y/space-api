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
import me.xap3y.space.dto.ImageDto;
import me.xap3y.space.dto.ImageInfoDto;
import me.xap3y.space.entity.DiscordConnection;
import me.xap3y.space.mapper.ImageInfoMapper;
import me.xap3y.space.service.DiscordConnectionService;
import me.xap3y.space.service.ImageService;
import me.xap3y.space.util.Utils;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Component
public class ImageCommand implements SlashCommand{

    private final ImageService imageService;
    private final ImageInfoMapper imageInfoMapper;
    private final Utils utils;
    private final DiscordConnectionService discordConnectionService;

    public ImageCommand(ImageService imageService, ImageInfoMapper imageInfoMapper, Utils utils, DiscordConnectionService discordConnectionService) {
        this.imageService = imageService;
        this.imageInfoMapper = imageInfoMapper;
        this.utils = utils;
        this.discordConnectionService = discordConnectionService;
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

        ImageDto imgDto;
        try {
            imgDto = imageService.getImage(uniqueId, false, true, false);
        } catch (Exception e) {
            return event.reply()
                    .withEphemeral(false)
                    .withContent(Emoji.ERROR.getUnicode() + " Image with UID `" + uniqueId + "` not found!")
                    .then();
        }

        ImageInfoDto imageInfoDto = imageInfoMapper.apply(Pair.of(uniqueId, imgDto));

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

        Optional<DiscordConnection> discordConnection = discordConnectionService.findByUserId(imgDto.uploader());

        if (discordConnection.isPresent()) {
            uploaderDiscord = " (" + Utils.structDiscordUserTag(discordConnection.get().getDiscordId()) + ")";
        }

        EmbedCreateSpec embed = EmbedCreateSpec.builder()
                .color(Color.YELLOW)
                .title(uniqueId + "." + imgDto.type() + " (" + imageInfoDto.size() + " KB)")
                /*.url(imageInfoDto.urlSet().portalUrl())*/
                .image(imageInfoDto.urlSet().customUrl()) // TODO
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
