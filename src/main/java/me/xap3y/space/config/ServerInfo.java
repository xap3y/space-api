package me.xap3y.space.config;

import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.UnknownHostException;

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

    public String getBaseUrl() {
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

    public String getFrontEndUrl() {
        return environment.getProperty("FRONTEND_URL", "http://127.0.0.1");
    }

    public String getShortImageUrl() {
        return environment.getProperty("SHORT_IMAGE_URL", "https://i.xap3y.tech");
    }

    public String getShortPasteUrl() {
        return environment.getProperty("SHORT_PASTE_URL", "https://p.xap3y.tech");
    }

    public String getShortShortenerUrl() {
        return environment.getProperty("SHORT_SHORTENER_URL", "https://r.xap3y.tech");
    }
}
