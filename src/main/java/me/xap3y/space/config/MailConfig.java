package me.xap3y.space.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

@Configuration
public class MailConfig {

    @Value("${mail.primary.host}")
    private String primaryHost;

    @Value("${mail.primary.port}")
    private int primaryPort;

    @Value("${mail.primary.username}")
    private String primaryUsername;

    @Value("${mail.primary.password}")
    private String primaryPassword;


    @Value("${mail.secondary.host}")
    private String secondaryHost;

    @Value("${mail.secondary.port}")
    private int secondaryPort;

    @Value("${mail.secondary.username}")
    private String secondaryUsername;

    @Value("${mail.secondary.password}")
    private String secondaryPassword;


    @Bean
    public JavaMailSenderImpl primaryMailSender() {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(primaryHost);
        sender.setPort(primaryPort);
        sender.setUsername(primaryUsername);
        sender.setPassword(primaryPassword);
        sender.setPort(465);
        sender.setProtocol("smtps");

        Properties props = sender.getJavaMailProperties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "false");

        return sender;
    }

    @Bean(name = "secondaryMailSender")
    public JavaMailSenderImpl secondaryMailSender() {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(secondaryHost);
        sender.setPort(secondaryPort);
        sender.setUsername(secondaryUsername);
        sender.setPassword(secondaryPassword);
        sender.setPort(465);
        sender.setProtocol("smtps");

        Properties props = sender.getJavaMailProperties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "false");

        return sender;
    }
}