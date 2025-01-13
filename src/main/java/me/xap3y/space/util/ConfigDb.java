package me.xap3y.space.util;

import lombok.Getter;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class ConfigDb {

    @Getter
    private final static String IMAGE_DIR = "images/";

    public final static File LOG_FILE = new File("logs.txt");

    public final static int MAX_PASTE_TEXT_LENGTH = 55045;

    public final static String[] BLACKLISTED_USERNAMES = {
            "nigga", "nigger", "bitch", "fucking"
    };

    @Getter
    private final static Map<String, String> redirectMapper = new HashMap<>() {{
        put("r.xap3y.tech", "%BASE%/v1/url/r/%PATH%");
        put("i.xap3y.tech", "%BASE%/v1/image/get/%PATH%");
    }};
}
