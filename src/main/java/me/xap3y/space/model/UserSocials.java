package me.xap3y.space.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserSocials implements Serializable {

    private String website;
    private String twitter;
    private String github;
    private String gitlab;
    private String discord;
    private String telegram;
    private String vk;
    private String facebook;
    private String instagram;
    private String youtube;
    private String twitch;
    private String steam;
    private String reddit;
    private String linkedin;
    private String tiktok;
    private String snapchat;
    private String whatsapp;
    private String soundcloud;
    private String spotify;
    private String threads;
    private String email;
    private String messenger;

    public UserSocials() {}
}
