package me.xap3y.space.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.xap3y.space.minigame.blackjack.model.Lobby;
import me.xap3y.space.minigame.blackjack.LobbyManager;
import me.xap3y.space.minigame.blackjack.model.Player;
import me.xap3y.space.util.Utils;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class BlackJackWebSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper mapper = new ObjectMapper();
    private final LobbyManager lobbyManager;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        var uri = session.getUri();
        if (uri == null) {
            session.close(CloseStatus.BAD_DATA);
            return;
        }

        // Using your existing Utils class assuming it parses query params
        String code = Utils.extractQueryParam(uri.getQuery(), "code");
        String token = Utils.extractQueryParam(uri.getQuery(), "token");

        Lobby lobby = lobbyManager.getLobby(code);

        // FIXED: Pointing to lobby.engine.players
        if (lobby == null || !lobby.engine.players.containsKey(token)) {
            session.close(new CloseStatus(4001, "Invalid lobby or token"));
            return;
        }

        Player player = lobby.engine.players.get(token);
        player.session = session;

        log.info("Player {} joined lobby {}", player.nickname, code);

        // Broadcast the updated state to everyone so the new player appears on their screens
        broadcastToLobby(lobby);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        Map<String, Object> payload = mapper.readValue(message.getPayload(), Map.class);
        String action = (String) payload.get("action");

        var uri = session.getUri();
        if (uri == null) return;

        String code = Utils.extractQueryParam(uri.getQuery(), "code");
        String token = Utils.extractQueryParam(uri.getQuery(), "token");

        Lobby lobby = lobbyManager.getLobby(code);
        if (lobby != null) {
            Player player = lobby.engine.players.get(token);
            if (player != null) {
                // Pass the action to the engine, and pass a callback that broadcasts the new state
                lobby.engine.broadcaster = () -> broadcastToLobby(lobby);
            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        var uri = session.getUri();
        if (uri == null) return;

        String code = Utils.extractQueryParam(uri.getQuery(), "code");
        String token = Utils.extractQueryParam(uri.getQuery(), "token");

        Lobby lobby = lobbyManager.getLobby(code);
        if (lobby != null) {
            Player player = lobby.engine.players.remove(token); // Remove player on disconnect
            if (player != null) {
                log.info("Player {} left lobby {}", player.nickname, code);
                broadcastToLobby(lobby); // Update remaining players
            }
        }
    }

    private void broadcastToLobby(Lobby lobby) {
        lobby.engine.players.values().forEach(player -> {
            if (player.session != null && player.session.isOpen()) {
                try {
                    // Generate personalized state (so `isMe` is true for the specific client receiving it)
                    Map<String, Object> state = lobby.engine.generateGameStateForPlayer(player.token);
                    sendJson(player.session, Map.of("type", "GAME_STATE", "state", state));
                } catch (IOException e) {
                    log.error("Failed to broadcast to player: " + player.nickname, e);
                }
            }
        });
    }

    private void sendJson(WebSocketSession session, Object obj) throws IOException {
        session.sendMessage(new TextMessage(mapper.writeValueAsString(obj)));
    }
}