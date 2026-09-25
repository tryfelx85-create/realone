package com.arena.anticheat;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Flags rapid block placement while looking sharply downward and moving
 * horizontally — the signature pattern of "scaffold" cheats that let
 * players bridge at full sprint speed without manually looking down for
 * each block.
 *
 * CAVEAT: legitimate fast bridgers (skilled manual "god bridging") can
 * occasionally trip this. It's tuned to require BOTH steep pitch AND
 * rapid consecutive placements to reduce false positives, but some
 * tolerance tuning against your playerbase is expected.
 */
public class ScaffoldCheck implements Listener {

    private static final float STEEP_PITCH = 60.0f;      // degrees looking down (90 = straight down)
    private static final long RAPID_PLACE_MS = 150;       // consecutive placements faster than this = suspicious
    private static final int RAPID_STREAK_REQUIRED = 5;   // consecutive rapid+steep placements before flag
    private static final double FLAG_THRESHOLD = 6.0;

    private final Map<UUID, Long> lastPlaceTime = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> rapidStreak = new ConcurrentHashMap<>();

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        UUID id = player.getUniqueId();
        long now = System.currentTimeMillis();

        float pitch = player.getLocation().getPitch();
        boolean lookingDown = pitch >= STEEP_PITCH;

        Long last = lastPlaceTime.get(id);
        lastPlaceTime.put(id, now);

        if (last != null && lookingDown && (now - last) < RAPID_PLACE_MS) {
            int streak = rapidStreak.merge(id, 1, Integer::sum);
            if (streak >= RAPID_STREAK_REQUIRED) {
                double total = ViolationManager.addViolation(player, ViolationManager.CheckType.SCAFFOLD, 3.0);
                rapidStreak.put(id, 0); // reset streak after flagging so we don't spam
                if (total >= FLAG_THRESHOLD) {
                    alertStaff(player, streak, total);
                }
            }
        } else {
            rapidStreak.put(id, 0); // streak broken - normal placement
        }
    }

    private void alertStaff(Player player, int streak, double total) {
        String msg = String.format("§c[AC] §f%s §7flagged for §cSCAFFOLD §7(%d rapid placements, score: %.1f)",
                player.getName(), streak, total);
        for (Player staff : Bukkit.getOnlinePlayers()) {
            if (staff.hasPermission("arena.anticheat.alerts")) {
                staff.sendMessage(msg);
            }
        }
        Bukkit.getLogger().warning("[AntiCheat] " + player.getName() + " flagged for SCAFFOLD: " + streak + " rapid placements");
    }
}
