package me.xap3y.space.minigame.blackjack.model;

import org.springframework.web.socket.WebSocketSession;

import java.util.ArrayList;
import java.util.List;

public class Player {
    public final String token;
    public final String nickname;
    public WebSocketSession session;
    public double bankroll = 1000.0;
    public List<Hand> hands = new ArrayList<>();

    // For tracking which hand the player is currently acting on (if they split)
    public int activeHandIndex = 0;
    public boolean isReadyForNextRound = false;

    public Player(String token, String nickname) {
        this.token = token;
        this.nickname = nickname;
    }
}