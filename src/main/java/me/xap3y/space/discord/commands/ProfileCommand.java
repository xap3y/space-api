package me.xap3y.space.discord.commands;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.Color;
import me.xap3y.space.api.exception.ResourceNotFoundException;
import me.xap3y.space.config.ServerInfo;
import me.xap3y.space.discord.Emoji;
import me.xap3y.space.dto.UserDto;
import me.xap3y.space.entity.DiscordConnection;
import me.xap3y.space.entity.User;
import me.xap3y.space.mapper.UserMapper;
import me.xap3y.space.service.DiscordConnectionService;
import me.xap3y.space.service.UserService;
import me.xap3y.space.util.Utils;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Component
public class ProfileCommand implements SlashCommand {

    private final DiscordConnectionService discordConnectionService;
    private final ServerInfo serverInfo;
    private final UserMapper userMapper;
    private final Utils utils;
    private final UserService userService;

    public ProfileCommand(DiscordConnectionService discordConnectionService, ServerInfo serverInfo, UserMapper userMapper, Utils utils, UserService userService) {
        this.discordConnectionService = discordConnectionService;
        this.serverInfo = serverInfo;
        this.userMapper = userMapper;
        this.utils = utils;
        this.userService = userService;
    }

    @Override
    public String getName() {
        return "profile";
    }

    @Override
    public Mono<Void> handle(ChatInputInteractionEvent event) {

        String username = event.getOption("username")
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::asString)
                .orElse(null);

        String userId = event.getInteraction().getUser().getId().asString();

        User user;

        if (username == null || username.isEmpty()) {
            Optional<DiscordConnection> connection = discordConnectionService.findByDiscordId(userId);

            if (connection.isEmpty()) {
                return event.reply(Emoji.ERROR.getUnicode() + " No user connected to this discord account!").then();
            }
            user = connection.get().getUserId();
        } else {
            Optional<User> foundUser = userService.tryFindByUsername(username);

            if (foundUser.isEmpty()) {
                return event.reply(Emoji.ERROR.getUnicode() + " User with username `" + username + "` not found!").then();
            }

            user = foundUser.get();
        }

        UserDto userDto = userMapper.apply(user);

        String invitedBy = userDto.invitor() != null ? utils.structDiscordProfileLink(userDto.invitor().getUsername()) : "N/A";

        EmbedCreateSpec embed = EmbedCreateSpec.builder()
                .color(Color.BLUE)
                .title(userDto.username())
                .url(serverInfo.getFrontEndUrl() + "/user/" + userDto.uid())
                .thumbnail(userDto.avatar())
                .addField( Emoji.UID.getUnicode() + " UID", userDto.uid() + "", true)
                .addField(Emoji.ROLE.getUnicode() + " Role", userDto.role().name(), true)
                .addField(Emoji.CALENDAR.getUnicode() + " Joined on", userDto.createdAt().format(DateTimeFormatter.ofPattern("yyyy/MM/d HH:mm:ss")), true)

                .addField(Emoji.IMAGE.getUnicode() + " Images", userDto.stats().getTotalUploads() + "", true)
                .addField(Emoji.PASTES.getUnicode() + " Pastes", userDto.stats().getPastesCreated() + "", true)
                .addField(Emoji.LINK.getUnicode() + " Urls", userDto.stats().getUrlsShortened() + "", true)

                .addField(Emoji.DATABASE.getUnicode() + " Storage Used", (userDto.stats().getStorageUsed() / 1024 / 1024) + "MB", true)
                .addField(Emoji.INVITOR.getUnicode() + " Invited By", invitedBy, true)
                .timestamp(Instant.now())
                .build();

        Button viewButton = Button.link(serverInfo.getFrontEndUrl() + "/user/" + userDto.username(), "View");

        return event.reply()
                .withEphemeral(false)
                .withEmbeds(embed)
                .withComponents(ActionRow.of(viewButton))
                .then();
    }
}
