package me.xap3y.space.minigame.blackjack.model;

import java.util.ArrayList;
import java.util.List;

public class Hand {
    public List<Card> cards = new ArrayList<>();
    public int bet;
    public boolean isDone = false;
    public boolean hasDoubled = false;
    public String result = null; // "win", "lose", "push", "blackjack"

    public Hand(int bet) {
        this.bet = bet;
    }
}