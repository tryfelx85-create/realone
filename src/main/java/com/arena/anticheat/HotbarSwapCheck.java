package com.arena.anticheat;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Flags "auto-swap" / "auto-totem" / "auto-eat" style cheats that
 * automatically move items into the hotbar (e.g. replacing a used totem,
 * or swapping in food) faster and more consistently than a human
 * manually dragging/hotkeying items.
 *
 * Detection signal: swaps into hotbar slots happening in rapid bursts
 * with very low timing variance, OR swaps happening within the same
 * tick/few-ms of a relevant trigger (e.g. right after taking damage or
 * right after a totem would have popped) — this simplified version
 * focuses on the timing-consistency signal, since trigger-correlation
 * needs hooking into damage/consumption events too.
 *
 * CAVEAT: players using physical macro keybinds (not "cheating" software,
 * but still automating swaps) will also trigger this — whether that's
 * against your rules is a policy decision, not a technical one.
 */
public class HotbarSwapCheck implements Listener {

    private static final int WINDOW_SIZE = 6;
    private static final double LOW_VARIANCE_MS = 15.0; // suspiciously uniform swap timing
    private static final long MIN_HUMAN_INTERVAL_MS = 80; // fastest plausible manual hotbar swap
    private static final double FLAG_THRESHOLD = 6.0;

    private final Map<UUID, Deque<Long>> swapTimestamps = new ConcurrentHashMap<>();

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getClickedInventory() == null) return;

        // Only care about swaps landing in the hotbar (slots 0-8 of the player's own inventory)
        boolean targetsHotbar = event.getClickedInventory().getType() == InventoryType.PLAYER
                && event.getSlot() >= 0 && event.getSlot() <= 8;

        // Also catch number-key hotbar swaps (moving an item from elsewhere into hotbar via hotkey)
        boolean isHotkeySwap = event.getClick().isKeyboardClick();

        if (!targetsHotbar && !isHotkeySwap) return;

        long now = System.currentTimeMillis();
        UUID id = player.getUniqueId();
        Deque<Long> timestamps = swapTimestamps.computeIfAbsent(id, k -> new ArrayDeque<>());
        timestamps.addLast(now);
        while (timestamps.size() > WINDOW_SIZE) {
            timestamps.removeFirst();
        }

        if (timestamps.size() < WINDOW_SIZE) return;

        Long[] times = timestamps.toArray(new Long[0]);
        double[] intervals = new double[times.length - 1];
        for (int i = 1; i < times.length; i++) {
            intervals[i - 1] = times[i] - times[i - 1];
        }

        double mean = mean(intervals);
        double stddev = stddev(intervals, mean);

        boolean tooFast = mean < MIN_HUMAN_INTERVAL_MS;
        boolean tooUniform = stddev < LOW_VARIANCE_MS;

        if (tooFast && tooUniform) {
            double total = ViolationManager.addViolation(player, ViolationManager.CheckType.HOTBAR_SWAP, 3.5);
            if (total >= FLAG_THRESHOLD) {
                alertStaff(player, mean, stddev, total);
            }
        }
    }

    private double mean(double[] values) {
        double sum = 0;
        for (double v : values) sum += v;
        return sum / values.length;
    }

    private double stddev(double[] values, double mean) {
        double sumSq = 0;
        for (double v : values) sumSq += (v - mean) * (v - mean);
        return Math.sqrt(sumSq / values.length);
    }

    private void alertStaff(Player player, double meanMs, double stddev, double total) {
        String msg = String.format("§c[AC] §f%s §7flagged for §cAUTO-HOTBAR-SWAP §7(avg %.0fms, stddev %.1fms, score: %.1f)",
                player.getName(), meanMs, stddev, total);
        for (Player staff : Bukkit.getOnlinePlayers()) {
            if (staff.hasPermission("arena.anticheat.alerts")) {
                staff.sendMessage(msg);
            }
        }
        Bukkit.getLogger().warning("[AntiCheat] " + player.getName() + " flagged for AUTO-HOTBAR-SWAP: avg "
                + meanMs + "ms, stddev " + stddev + "ms");
    }
}
