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

}
