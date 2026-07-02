package me.xap3y.space.minigame.blackjack;

import me.xap3y.space.minigame.blackjack.model.Lobby;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Random;

@Component
public class LobbyManager {
    // Key: 3-digit code, Value: Lobby
    private final ConcurrentHashMap<String, Lobby> lobbies = new ConcurrentHashMap<>();
    private final Random random = new Random();

    public String createLobby() {
        String code;
        do {
            code = String.format("%03d", random.nextInt(1000));
        } while (lobbies.containsKey(code));

        lobbies.put(code, new Lobby(code));
        return code;
    }

    public Lobby getLobby(String code) {
        return lobbies.get(code);
    }
}