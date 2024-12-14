package me.xap3y.space.util;

import lombok.Getter;

public class ConfigDb {

    @Getter
    private final static String IMAGE_DIR = "images/";

    public final static int MAX_PASTE_TEXT_LENGTH = 55045;

    public final static String[] BLACKLISTED_USERNAMES = {
            "nigga", "nigger", "bitch", "fucking", ""
    };
}
