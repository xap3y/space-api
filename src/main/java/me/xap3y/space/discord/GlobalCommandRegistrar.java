package me.xap3y.space.discord;

import discord4j.common.JacksonResources;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.rest.RestClient;
import discord4j.rest.service.ApplicationService;
import lombok.extern.slf4j.Slf4j;
import me.xap3y.space.config.ServerInfo;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@ConditionalOnProperty(name = "USE_DISCORD_BOT", havingValue = "true", matchIfMissing = false)
public class GlobalCommandRegistrar implements ApplicationRunner {

    private final RestClient client;
    private final ServerInfo serverInfo;

    public GlobalCommandRegistrar(RestClient client, ServerInfo serverInfo) {
        this.client = client;
        this.serverInfo = serverInfo;
    }

    @Override
    public void run(ApplicationArguments args) throws IOException {
        //Create an ObjectMapper that supported Discord4J classes
        final JacksonResources d4jMapper = JacksonResources.create();

        // Convenience variables for the sake of easier to read code below.
        PathMatchingResourcePatternResolver matcher = new PathMatchingResourcePatternResolver();
        final ApplicationService applicationService = client.getApplicationService();
        final long applicationId = client.getApplicationId().block();

        List<ApplicationCommandRequest> commands = new ArrayList<>();
        for (Resource resource : matcher.getResources("commands/*.json")) {
            ApplicationCommandRequest request = d4jMapper.getObjectMapper()
                    .readValue(resource.getInputStream(), ApplicationCommandRequest.class);

            commands.add(request);
        }

        /* Bulk overwrite commands. This is now idempotent, so it is safe to use this even when only 1 command
        is changed/added/removed
        */
        applicationService.bulkOverwriteGuildApplicationCommand(applicationId, serverInfo.getRemoteDiscordBotGuildId(), commands)
                .doOnNext(ignore -> log.info("Successfully registered Guild Command"))
                .doOnError(e -> log.error("Failed to register global commands", e))
                .subscribe();

        /*applicationService.getGlobalApplicationCommands(applicationId)
                .collectList()
                .block()
                .stream()
                .filter(cmd -> cmd.name().equals("ping"))
                .findFirst()
                .ifPresent(cmd -> {
                    applicationService.deleteGlobalApplicationCommand(applicationId, cmd.id().asLong())
                            .block();
                    System.out.println("Deleted command: " + cmd.name());
                });*/
    }
}
