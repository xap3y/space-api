package me.xap3y.space.config;

import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Objects;

@Component
public class ServerInfo {

    private final Environment environment;

    private final ServletWebServerApplicationContext server;

    public ServerInfo(ServletWebServerApplicationContext server, Environment environment) {
        this.server = server;
        this.environment = environment;
    }

    public int getPort() {
        return server.getWebServer().getPort();
    }

    public String getHost() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            return environment.getProperty("server.host", "localhost");
        }
    }

    public String getProtocol() {
        return environment.getProperty("server.protocol", "test");
    }

    public String  getBaseUrl() {
        return environment.getProperty("server.protocol", "http") +
                "://" +
                environment.getProperty("server.baseurl", "127.0.0.1");
    }

    public String getTestCorsUrl() {
        return environment.getProperty("cors.test.url", "https://demo.xap3y.tech");
    }

    public String getEnv() {
        return environment.getProperty("spring.enviroment", "dev");
    }

    public String getDiscordBotToken() {
        return environment.getProperty("DISCORD_BOT");
    }

    public String getTelegramBotToken() {
        return environment.getProperty("TELEGRAM_BOT_TOKEN");
    }

    public String getTelegramVerifyBotToken() {
        return environment.getProperty("TELEGRAM_VERIFY_BOT_TOKEN");
    }

    public String getDiscordClientId() {
        return environment.getProperty("DISCORD_BOT_ID");
    }

    public String getDiscordAuthBotToken() {
        return environment.getProperty("DISCORD_CLIENT_SECRET");
    }

    public String getRemoteDiscordBotToken() {
        return environment.getProperty("REMOTE_DISCORD_BOT_TOKEN");
    }

    public Long getRemoteDiscordBotGuildId() {
        return Long.valueOf(Objects.requireNonNull(environment.getProperty("REMOTE_DISCORD_BOT_GUILD_ID")));
    }

    public Long getRemoteDiscordBotChannelId() {
        return Long.valueOf(Objects.requireNonNull(environment.getProperty("REMOTE_DISCORD_BOT_CHANNEL_ID")));
    }

    public String getNamespaceName() {
        return environment.getProperty("NAMESPACE_TAG", "local");
    }

    public String getFrontEndUrl() {
        return environment.getProperty("FRONTEND_URL", "http://127.0.0.1");
    }

    public String getShortImageUrl() {
        return environment.getProperty("SHORT_IMAGE_URL", "https://i.xap3y.tech");
    }

    public String getShortPasteUrl() {
        return environment.getProperty("SHORT_PASTE_URL", "https://p.xap3y.tech");
    }

    public Boolean getUseDiscordWebhook() {
        return environment.getProperty("USE_DISCORD_WEBHOOK", Boolean.class, false);
    }

    public Boolean getUseDiscordBot() {
        return environment.getProperty("USE_DISCORD_BOT", Boolean.class, false);
    }

    public Boolean getUseTelegramBot() {
        return environment.getProperty("USE_TELEGRAM_BOT", Boolean.class, false);
    }

    public Boolean getUseTelegramVerifyBot() {
        return environment.getProperty("USE_TELEGRAM_VERIFY_BOT", Boolean.class, false);
    }

    public String getShortShortenerUrl() {
        return environment.getProperty("SHORT_SHORTENER_URL", "https://r.xap3y.tech");
    }

    public String getInboundEmailToken() {
        return environment.getProperty("INBOUND_EMAIL_TOKEN", "password");
    }

    public String getInboundEmailAddress() {
        return environment.getProperty("INBOUND_EMAIL_ADDRESS", "xap3y.space");
    }

    public Integer getAuthCookieMaxAge() {
        return Integer.valueOf(environment.getProperty("AUTH_COOKIE_MAXAGE", "604800"));
    }

    public String getAuthCookieSameSite() {
        return environment.getProperty("AUTH_COOKIE_SAMESITE", "None");
    }

    public Boolean getAuthCookieSecure() {
        return environment.getProperty("AUTH_COOKIE_SECURE", Boolean.class, true);
    }
}
