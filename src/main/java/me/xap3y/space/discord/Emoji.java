package me.xap3y.space.discord;

import lombok.Getter;

@Getter
public enum Emoji {
    CHECK("<a:check_mark:1383403908893900941>"),
    ERROR("<a:x_mark:1383403844545024070>"),
    WARNING("⚠️"),
    DATABASE("<:database:1383444794662977537>"),
    UID("\uD83C\uDFF7"),
    IMAGE("\uD83D\uDDBC️"),
    LINK("\uD83D\uDD17"),
    PASTES("\uD83D\uDCC4"),
    CALENDAR("\uD83D\uDCC5"),
    ROLE("\uD83E\uDDF0"),
    INVITOR("\uD83D\uDC64"),
    INFO("ℹ️");

    private final String unicode;

    Emoji(String unicode) {
        this.unicode = unicode;
    }
}
