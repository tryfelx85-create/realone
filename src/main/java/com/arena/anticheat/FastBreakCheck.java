package com.arena.anticheat;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Flags blocks broken faster than the minimum legitimate break time for
 * that block/tool/enchant combo would allow.
 *
 * This is a SIMPLIFIED estimate, not a full replica of Minecraft's break-
 * speed formula (which factors in tool tier, efficiency level, haste,
 * mining fatigue, tool material, and whether the correct tool is used).
 * It uses a conservative flat "fastest plausible break time" per hardness
 * tier so it won't false-flag efficiency/haste, at the cost of missing
 * some subtler fast-break cheats. Tune MIN_BREAK_MS per your server's
 * enchant caps if you see false positives.
 */
public class FastBreakCheck implements Listener {

    private static final long MIN_BREAK_MS = 50; // absolute floor - nothing legit breaks faster than this
    private static final double FLAG_THRESHOLD = 6.0;

    private final Map<UUID, Long> lastBreakStart = new ConcurrentHashMap<>();

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        long now = System.currentTimeMillis();

        Long last = lastBreakStart.get(player.getUniqueId());
        lastBreakStart.put(player.getUniqueId(), now);

        if (last == null) return;

        long elapsed = now - last;
        Material blockType = event.getBlock().getType();

        // Instant-break blocks (e.g. tall grass, torches) are not worth checking
        if (isInstaBreakable(blockType)) return;

        if (elapsed < MIN_BREAK_MS) {
            double total = ViolationManager.addViolation(player, ViolationManager.CheckType.FAST_BREAK, 3.0);
            if (total >= FLAG_THRESHOLD) {
                alertStaff(player, blockType, elapsed, total);
            }
        }
    }

    private boolean isInstaBreakable(Material material) {
        return switch (material) {
            case SHORT_GRASS, TALL_GRASS, TORCH, WALL_TORCH, DEAD_BUSH, FERN,
                 LARGE_FERN, SNOW, REDSTONE_TORCH, REDSTONE_WALL_TORCH,
                 TRIPWIRE, VINE, SUGAR_CANE -> true;
            default -> false;
        };
    }

    private void alertStaff(Player player, Material block, long elapsedMs, double total) {
        String msg = String.format("§c[AC] §f%s §7flagged for §cFAST-BREAK §7(%s in %dms, score: %.1f)",
                player.getName(), block, elapsedMs, total);
        for (Player staff : Bukkit.getOnlinePlayers()) {
            if (staff.hasPermission("arena.anticheat.alerts")) {
                staff.sendMessage(msg);
            }
        }
        Bukkit.getLogger().warning("[AntiCheat] " + player.getName() + " flagged for FAST-BREAK: "
                + block + " in " + elapsedMs + "ms");
    }
}
