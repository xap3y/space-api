package me.xap3y.space.minigame.blackjack;

import me.xap3y.space.minigame.blackjack.model.*;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Slf4j
public class GameEngine {
    private final String lobbyCode;
    public final ConcurrentHashMap<String, Player> players = new ConcurrentHashMap<>();

    public String phase = "waiting"; // waiting, betting, dealing, playerTurn, dealerTurn, roundEnd
    public String message = "Waiting for players to join...";
    public String hostToken = null;

    // Turn & Timer State
    public List<String> seatOrder = new ArrayList<>();
    public String currentPlayerToken = null;
    private ScheduledFuture<?> actionTimer;
    public long actionDeadline = 0;
    public Runnable broadcaster; // Callback to WS handler

    public List<Card> dealerCards = new ArrayList<>();
    private List<Card> deck = new ArrayList<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

    public GameEngine(String lobbyCode) {
        this.lobbyCode = lobbyCode;
        initDeck();
    }

    private void initDeck() {
        deck.clear();
        int idCounter = 0;
        for (Suit suit : Suit.values()) {
            for (Rank rank : Rank.values()) {
                deck.add(new Card(suit, rank, rank.symbol + "-" + suit.symbol + "-" + (++idCounter)));
            }
        }
        Collections.shuffle(deck);
    }

    private Card drawCard() {
        if (deck.size() < 15) initDeck();
        return deck.remove(deck.size() - 1);
    }

    public static Score scoreHand(List<Card> cards) {
        int total = 0, aces = 0;
        for (Card c : cards) {
            total += c.rank().value;
            if (c.rank() == Rank.ACE) aces++;
        }
        boolean soft = false;
        while (total > 21 && aces > 0) { total -= 10; aces--; }
        if (aces > 0 && total <= 21) soft = true;
        return new Score(total, soft);
    }

    public static boolean isBlackjack(List<Card> cards) {
        return cards.size() == 2 && scoreHand(cards).total() == 21;
    }

    // --- Timer Logic ---
    private void startTimer(int seconds, Runnable onTimeout) {
        if (actionTimer != null) actionTimer.cancel(false);
        actionDeadline = System.currentTimeMillis() + (seconds * 1000L);
        actionTimer = scheduler.schedule(() -> {
            synchronized (this) { onTimeout.run(); }
        }, seconds, TimeUnit.SECONDS);
    }

    private void cancelTimer() {
        if (actionTimer != null) {
            actionTimer.cancel(false);
            actionTimer = null;
            actionDeadline = 0;
        }
    }

    public synchronized void removePlayer(String token) {
        players.remove(token);
        seatOrder.remove(token);
        if (token.equals(currentPlayerToken)) {
            advanceTurn();
            if (broadcaster != null) broadcaster.run();
        }
    }

    // --- Action Handler ---
    public synchronized void handleAction(Player player, String action, Map<String, Object> payload) {
        try {
            switch (action.toUpperCase()) {
                case "START_GAME" -> {
                    if (phase.equals("waiting") && player.token.equals(hostToken)) {
                        startBettingPhase();
                    }
                }
                case "BET" -> {
                    if (!phase.equals("betting")) return;
                    if (player.hands.size() > 0) return; // Already bet

                    int amount = payload.containsKey("amount") ? (int) payload.get("amount") : 10;
                    if (player.bankroll < amount) amount = (int) player.bankroll;

                    player.bankroll -= amount;
                    player.hands.add(new Hand(amount));

                    // Check if everyone has bet
                    if (players.values().stream().allMatch(p -> !p.hands.isEmpty())) {
                        startDealing();
                    }
                }
                case "HIT" -> {
                    if (!phase.equals("playerTurn") || !player.token.equals(currentPlayerToken)) return;
                    Hand activeHand = player.hands.get(player.activeHandIndex);
                    if (!activeHand.isDone) {
                        activeHand.cards.add(drawCard());
                        if (scoreHand(activeHand.cards).total() >= 21) {
                            activeHand.isDone = true;
                            advanceTurn();
                        } else {
                            startPlayerTurnTimer(player.token); // Reset timer for next move
                        }
                    }
                }
                case "STAND" -> {
                    if (!phase.equals("playerTurn") || !player.token.equals(currentPlayerToken)) return;
                    player.hands.get(player.activeHandIndex).isDone = true;
                    advanceTurn();
                }
                case "DOUBLE" -> {
                    if (!phase.equals("playerTurn") || !player.token.equals(currentPlayerToken)) return;
                    Hand activeHand = player.hands.get(player.activeHandIndex);
                    if (activeHand.cards.size() == 2 && player.bankroll >= activeHand.bet) {
                        player.bankroll -= activeHand.bet;
                        activeHand.bet *= 2;
                        activeHand.hasDoubled = true;
                        activeHand.cards.add(drawCard());
                        activeHand.isDone = true;
                        advanceTurn();
                    }
                }
                case "READY" -> {
                    if (!phase.equals("roundEnd")) return;
                    player.isReadyForNextRound = true;
                    if (players.values().stream().allMatch(p -> p.isReadyForNextRound)) {
                        startBettingPhase();
                    }
                }
            }
        } finally {
            if (broadcaster != null) broadcaster.run();
        }
    }

    private void startBettingPhase() {
        phase = "betting";
        message = "Place your bets! (15s)";
        dealerCards.clear();
        for (Player p : players.values()) {
            p.hands.clear();
            p.isReadyForNextRound = false;
            p.activeHandIndex = 0;
            if (p.bankroll <= 0) p.bankroll = 100.0; // Top up bankrupt players
        }

        startTimer(15, () -> {
            // Auto-bet $10 for anyone who didn't bet to keep game moving
            for (Player p : players.values()) {
                if (p.hands.isEmpty()) {
                    handleAction(p, "BET", Map.of("amount", 10));
                }
            }
        });
    }

    private void startDealing() {
        cancelTimer();
        phase = "dealing";
        message = "Dealing cards...";

        // Establish turn order based on who is in the lobby
        seatOrder = new ArrayList<>(players.keySet());

        dealerCards.clear();
        dealerCards.add(drawCard());
        dealerCards.add(drawCard());

        for (String token : seatOrder) {
            Player p = players.get(token);
            p.hands.get(0).cards.add(drawCard());
            p.hands.get(0).cards.add(drawCard());
        }

        phase = "playerTurn";
        currentPlayerToken = seatOrder.get(0);
        checkCurrentPlayerState();
    }

    private void checkCurrentPlayerState() {
        Player p = players.get(currentPlayerToken);
        if (p == null || isBlackjack(p.hands.get(0).cards)) {
            advanceTurn();
        } else {
            message = "Waiting for " + p.nickname + "...";
            startPlayerTurnTimer(currentPlayerToken);
        }
    }

    private void startPlayerTurnTimer(String token) {
        startTimer(15, () -> {
            Player p = players.get(token);
            if (p != null) handleAction(p, "STAND", Map.of());
        });
    }

    private void advanceTurn() {
        cancelTimer();
        Player p = players.get(currentPlayerToken);

        // Handle Splits (advance hand index instead of player if needed)
        if (p != null && p.activeHandIndex < p.hands.size() - 1) {
            p.activeHandIndex++;
            startPlayerTurnTimer(currentPlayerToken);
            return;
        }

        int currentIndex = seatOrder.indexOf(currentPlayerToken);
        if (currentIndex < seatOrder.size() - 1) {
            currentPlayerToken = seatOrder.get(currentIndex + 1);
            checkCurrentPlayerState();
        } else {
            currentPlayerToken = null;
            runDealerAI();
        }
    }

    private void runDealerAI() {
        phase = "dealerTurn";
        message = "Dealer's turn...";
        scheduler.schedule(() -> {
            try {
                Score dScore = scoreHand(dealerCards);
                while (dScore.total() < 17 || (dScore.total() == 17 && dScore.soft())) {
                    dealerCards.add(drawCard());
                    dScore = scoreHand(dealerCards);
                    if (broadcaster != null) broadcaster.run();
                    Thread.sleep(800);
                }
                synchronized (this) { finalizeRound(); }
            } catch (InterruptedException e) { log.error("Dealer interrupted", e); }
        }, 1000, TimeUnit.MILLISECONDS);
    }

    private void finalizeRound() {
        Score dealerScore = scoreHand(dealerCards);
        boolean dealerBJ = isBlackjack(dealerCards);

        for (Player p : players.values()) {
            for (Hand h : p.hands) {
                Score pScore = scoreHand(h.cards);
                boolean playerBJ = isBlackjack(h.cards);

                if (playerBJ && !dealerBJ) { h.result = "blackjack"; p.bankroll += h.bet * 2.5; }
                else if (playerBJ && dealerBJ) { h.result = "push"; p.bankroll += h.bet; }
                else if (!playerBJ && dealerBJ) { h.result = "lose"; }
                else if (pScore.total() > 21) { h.result = "bust"; }
                else if (dealerScore.total() > 21) { h.result = "win"; p.bankroll += h.bet * 2; }
                else if (pScore.total() > dealerScore.total()) { h.result = "win"; p.bankroll += h.bet * 2; }
                else if (pScore.total() < dealerScore.total()) { h.result = "lose"; }
                else { h.result = "push"; p.bankroll += h.bet; }
            }
        }
        phase = "roundEnd";
        message = "Round complete. Waiting for players to ready up...";
        startTimer(15, () -> { synchronized (this) { startBettingPhase(); if (broadcaster != null) broadcaster.run(); } });
    }

    // --- FIX FOR "SIXDIAMONDS" Frontend issue ---
    private Map<String, Object> formatCard(Card c) {
        if (c.id().equals("hidden")) return Map.of("suit", "♠", "rank", "A", "id", "hidden");
        return Map.of("suit", c.suit().symbol, "rank", c.rank().symbol, "id", c.id());
    }

    public Map<String, Object> generateGameStateForPlayer(String playerToken) {
        List<Map<String, Object>> playersData = new ArrayList<>();

        // Keep players in deterministic seat order if game started, else random
        List<Player> orderedPlayers = seatOrder.isEmpty()
                ? new ArrayList<>(players.values())
                : seatOrder.stream().map(players::get).filter(Objects::nonNull).collect(Collectors.toList());

        for (Player p : orderedPlayers) {
            List<Map<String, Object>> handsData = new ArrayList<>();
            for (Hand h : p.hands) {
                handsData.add(Map.of(
                        "cards", h.cards.stream().map(this::formatCard).collect(Collectors.toList()),
                        "score", scoreHand(h.cards),
                        "bet", h.bet,
                        "result", h.result == null ? "pending" : h.result,
                        "isDone", h.isDone
                ));
            }
            playersData.add(Map.of(
                    "nickname", p.nickname,
                    "isMe", p.token.equals(playerToken),
                    "isCurrentTurn", p.token.equals(currentPlayerToken),
                    "hasBet", !p.hands.isEmpty(),
                    "bankroll", p.bankroll,
                    "hands", handsData,
                    "activeHandIndex", p.activeHandIndex
            ));
        }

        List<Map<String, Object>> visibleDealerCards = new ArrayList<>();
        if (!dealerCards.isEmpty()) {
            visibleDealerCards.add(formatCard(dealerCards.get(0)));
            if (phase.equals("dealerTurn") || phase.equals("roundEnd")) {
                visibleDealerCards.addAll(dealerCards.subList(1, dealerCards.size()).stream().map(this::formatCard).collect(Collectors.toList()));
            } else {
                visibleDealerCards.add(formatCard(new Card(Suit.SPADES, Rank.ACE, "hidden")));
            }
        }

        return Map.of(
                "phase", phase,
                "message", message,
                "dealerCards", visibleDealerCards,
                "dealerScore", phase.equals("dealerTurn") || phase.equals("roundEnd")
                        ? scoreHand(dealerCards)
                        : (visibleDealerCards.isEmpty() ? Map.of("total", 0, "soft", false) : Map.of("total", scoreHand(List.of(dealerCards.get(0))).total(), "soft", false)),
                "revealDealer", phase.equals("dealerTurn") || phase.equals("roundEnd"),
                "isHost", hostToken != null && hostToken.equals(playerToken),
                "actionDeadline", actionDeadline,
                "players", playersData
        );
    }
}