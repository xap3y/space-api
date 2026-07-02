package me.xap3y.space.controller.minigames;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.xap3y.space.minigame.blackjack.model.Lobby;
import me.xap3y.space.minigame.blackjack.LobbyManager;
import me.xap3y.space.minigame.blackjack.model.Player;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/v1/minigame/blackjack")
@RequiredArgsConstructor
public class BlackJackController {

    private final LobbyManager lobbyManager;

    @PostMapping("/create")
    public ResponseEntity<?> createLobby(@RequestBody CreateRequest req) {
        if (req.nickname() == null || req.nickname().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Nickname required"));
        }

        String code = lobbyManager.createLobby();
        Lobby lobby = lobbyManager.getLobby(code);

        // Instantly join the creator as the host
        String wsToken = UUID.randomUUID().toString();
        Player player = new Player(wsToken, req.nickname());
        lobby.engine.players.put(wsToken, player);
        lobby.engine.hostToken = wsToken; // Mark them as the host

        return ResponseEntity.ok(Map.of("token", wsToken, "code", code));
    }

    @PostMapping("/join")
    public ResponseEntity<?> joinLobby(@RequestBody JoinRequest req) {
        Lobby lobby = lobbyManager.getLobby(req.code());
        if (lobby == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Lobby not found"));
        }

        String wsToken = UUID.randomUUID().toString();
        Player player = new Player(wsToken, req.nickname());
        lobby.engine.players.put(wsToken, player);

        return ResponseEntity.ok(Map.of("token", wsToken, "code", req.code()));
    }

    public record CreateRequest(String nickname) {}
    public record JoinRequest(String code, String nickname) {}
}