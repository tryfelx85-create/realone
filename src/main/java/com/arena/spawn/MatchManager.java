package com.arena.spawn;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class MatchManager {

    private static final Set<UUID> activeFighters = new HashSet<>();
    private static UUID player1;
    private static UUID player2;

    public static void startMatch(UUID p1, UUID p2) {
        activeFighters.clear();
        activeFighters.add(p1);
        activeFighters.add(p2);
        player1 = p1;
        player2 = p2;
    }

    public static void endMatch() {
        activeFighters.clear();
        player1 = null;
        player2 = null;
    }

    public static boolean isFighting(UUID uuid) {
        return activeFighters.contains(uuid);
    }

    public static boolean isMatchActive() {
        return !activeFighters.isEmpty();
    }

    public static UUID getPlayer1() {
        return player1;
    }

    public static UUID getPlayer2() {
        return player2;
    }
}
