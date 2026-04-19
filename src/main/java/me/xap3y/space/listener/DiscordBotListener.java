package me.xap3y.space.listener;

import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import lombok.extern.slf4j.Slf4j;
import me.xap3y.space.discord.commands.SlashCommand;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@ConditionalOnProperty(name = "USE_DISCORD_BOT", havingValue = "true", matchIfMissing = false)
public class DiscordBotListener {

    private final Collection<SlashCommand> commands;

    public DiscordBotListener(List<SlashCommand> slashCommands, Optional<GatewayDiscordClient> client) {
        commands = slashCommands;

        if (client.isEmpty()) {
            log.warn("Discord bot client is not available. Listeners will not be registered.");
            return;
        }

        GatewayDiscordClient discordClient = client.get();
        discordClient.on(ChatInputInteractionEvent.class, this::handle).subscribe();
        discordClient.on(ReadyEvent.class)
            .subscribe(event -> {
                log.info("Discord bot is ready! Logged in as: {}", event.getSelf().getUsername());
            });
        discordClient.on(MessageCreateEvent.class).subscribe(event -> {
            Message message = event.getMessage();
            if (message.getContent().equals("!ping")) {
                message.getChannel().block().createMessage("Pong2!").block();
            }
        });
    }

    public Mono<Void> handle(ChatInputInteractionEvent event) {
        //Convert our list to a flux that we can iterate through
        return Flux.fromIterable(commands)
                //Filter out all commands that don't match the name this event is for
                .filter(command -> command.getName().equals(event.getCommandName()))
                //Get the first (and only) item in the flux that matches our filter
                .next()
                //Have our command class handle all logic related to its specific command.
                .flatMap(command -> command.handle(event));
    }
}
