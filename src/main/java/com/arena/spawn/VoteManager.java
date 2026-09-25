package com.arena.spawn;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class VoteManager {

    // player UUID -> kit choice (1, 2, or 3)
    private static final Map<UUID, Integer> votes = new HashMap<>();
    private static final Random random = new Random();
    private static BukkitTask countdownTask;
    private static boolean resolved = false;

    public static void reset() {
        votes.clear();
        resolved = false;
        if (countdownTask != null) {
            countdownTask.cancel();
            countdownTask = null;
        }
    }

    public static void startTimer(ArenaPlugin plugin) {
        countdownTask = Bukkit.getScheduler().runTaskLater(plugin, VoteManager::onTimeUp, 20L * 30); // 30 seconds
    }

    public static void castVote(Player player, int kitChoice) {
        if (resolved) return;

        // FIX: reject invalid kit choices instead of silently accepting them
        if (kitChoice < 1 || kitChoice > 3) {
            player.sendMessage("§cInvalid kit choice.");
            return;
        }

        votes.put(player.getUniqueId(), kitChoice);

        UUID p1 = MatchManager.getPlayer1();
        UUID p2 = MatchManager.getPlayer2();

        if (votes.containsKey(p1) && votes.containsKey(p2)) {
            int vote1 = votes.get(p1);
            int vote2 = votes.get(p2);

            // Only resolve early if both agree; if they differ, wait for the timer
            // and then randomly pick between their two choices.
            if (vote1 == vote2) {
                resolveVote(p1, p2, vote1, false);
            } else {
                Player player1 = Bukkit.getPlayer(p1);
                Player player2 = Bukkit.getPlayer(p2);
                if (player1 != null) player1.sendMessage("§7Votes differ — waiting for the timer to decide...");
                if (player2 != null) player2.sendMessage("§7Votes differ — waiting for the timer to decide...");
            }
        }
    }

    private static void onTimeUp() {
        if (resolved) return;

        UUID p1 = MatchManager.getPlayer1();
        UUID p2 = MatchManager.getPlayer2();
        if (p1 == null || p2 == null) return;

        // Default anyone who didn't vote to Kit 1
        int vote1 = votes.getOrDefault(p1, 1);
        int vote2 = votes.getOrDefault(p2, 1);

        int finalKit = (vote1 == vote2) ? vote1 : (random.nextBoolean() ? vote1 : vote2);
        resolveVote(p1, p2, finalKit, vote1 != vote2);
    }

    private static void resolveVote(UUID p1, UUID p2, int finalKit, boolean wasRandomTiebreak) {
        resolved = true;
        if (countdownTask != null) {
            countdownTask.cancel();
            countdownTask = null;
        }

        Player player1 = Bukkit.getPlayer(p1);
        Player player2 = Bukkit.getPlayer(p2);

        if (player1 != null) Kits.giveKitByChoice(player1, finalKit);
        if (player2 != null) Kits.giveKitByChoice(player2, finalKit);

        String kitName = switch (finalKit) {
            case 1 -> "Sword & Steaks";
            case 2 -> "Sword, Axe & Shield";
            case 3 -> "Healing Potions";
            default -> "Unknown";
        };

        String resultMessage = wasRandomTiebreak
                ? "§eTime's up! Votes differed — kit chosen at random: §f" + kitName
                : "§aBoth players chose the same kit: §f" + kitName;

        if (player1 != null) {
            player1.sendMessage(resultMessage);
            player1.sendMessage("§c§lFIGHT!");
        }
        if (player2 != null) {
            player2.sendMessage(resultMessage);
            player2.sendMessage("§c§lFIGHT!");
        }

        votes.clear();
    }
}
