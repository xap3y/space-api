package me.xap3y.space.minigame.blackjack.model;

import me.xap3y.space.minigame.blackjack.GameEngine;

import java.util.concurrent.ConcurrentHashMap;

public class Lobby {
    public final String code;
    public final GameEngine engine; // <-- Here is the engine!

    public Lobby(String code) {
        this.code = code;
        this.engine = new GameEngine(code);
    }
}