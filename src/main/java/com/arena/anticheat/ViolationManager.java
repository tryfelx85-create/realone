package com.arena.anticheat;

import org.bukkit.entity.Player;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Tracks violation "points" per player per check type. Points decay over
 * time so a single old flag doesn't permanently brand a player. When a
 * player crosses a check's alert threshold, staff get notified in chat
 * (swap this for a webhook/log call if you want it elsewhere).
 */
public class ViolationManager {

    public enum CheckType {
        REACH, CPS, FAST_BREAK, SCAFFOLD, SPEED, HOTBAR_SWAP
    }

    private static final Map<UUID, Map<CheckType, Double>> violations = new ConcurrentHashMap<>();
    private static Logger logger;

    public static void init(Logger pluginLogger) {
        logger = pluginLogger;
    }

    /**
     * Add violation points for a player on a given check. Returns the
     * player's current total for that check after adding.
     */
    public static double addViolation(Player player, CheckType type, double points) {
        Map<CheckType, Double> playerMap = violations.computeIfAbsent(player.getUniqueId(), k -> new ConcurrentHashMap<>());
        double newTotal = playerMap.merge(type, points, Double::sum);

        if (logger != null) {
            logger.info(String.format("[AntiCheat] %s -> %s: +%.1f (total: %.1f)",
                    player.getName(), type, points, newTotal));
        }
        return newTotal;
    }

    /** Slowly decays a player's score for a check — call this periodically (e.g. every few seconds) per player. */
    public static void decay(Player player, CheckType type, double amount) {
        Map<CheckType, Double> playerMap = violations.get(player.getUniqueId());
        if (playerMap == null) return;
        playerMap.computeIfPresent(type, (k, v) -> Math.max(0, v - amount));
    }

    public static double getViolations(Player player, CheckType type) {
        Map<CheckType, Double> playerMap = violations.get(player.getUniqueId());
        if (playerMap == null) return 0;
        return playerMap.getOrDefault(type, 0.0);
    }

    public static void reset(Player player) {
        violations.remove(player.getUniqueId());
    }

    public static void clearAll() {
        violations.clear();
    }
}
