package com.arena.anticheat;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.potion.PotionEffectType;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Basic horizontal-speed check. THIS IS THE WEAKEST/MOST FALSE-POSITIVE-
 * PRONE CHECK IN THE SET. Real movement speed is affected by: sprint,
 * potion effects (speed/slowness), ice, soul sand, water, elytra, knock-
 * back, riptide, server tick lag, and more. This implementation only
 * accounts for sprint + speed potions + basic elytra/water exemption, and
 * uses a generous multiplier on top of that.
 *
 * Recommendation: treat this check as a LOW-CONFIDENCE SIGNAL. Don't
 * auto-punish off it alone — use it to flag players for a staff look,
 * combined with other checks.
 */
public class SpeedCheck implements Listener {

    private static final double BASE_WALK_SPEED = 4.317;   // blocks/sec, vanilla walk
    private static final double SPRINT_MULTIPLIER = 1.3;
    private static final double LENIENCY_MULTIPLIER = 1.6; // generous buffer for lag/knockback/etc.
    private static final double FLAG_THRESHOLD = 8.0;

    private final Map<UUID, Long> lastCheck = new ConcurrentHashMap<>();
    private final Map<UUID, org.bukkit.Location> lastLocation = new ConcurrentHashMap<>();

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        UUID id = player.getUniqueId();
        long now = System.currentTimeMillis();

        // Skip players who are flying (creative/spectator/elytra), in water, or falling —
        // too many legitimate speed variables to check reliably here.
        if (player.isFlying() || player.isGliding() || player.isInWater() || !player.isOnGround()) {
            lastLocation.put(id, player.getLocation());
            lastCheck.put(id, now);
            return;
        }

        org.bukkit.Location prev = lastLocation.get(id);
        Long prevTime = lastCheck.get(id);
        lastLocation.put(id, player.getLocation());
        lastCheck.put(id, now);

        if (prev == null || prevTime == null) return;
        double elapsedSec = (now - prevTime) / 1000.0;
        if (elapsedSec <= 0 || elapsedSec > 1.0) return; // skip if too much time passed (teleport, lag spike, etc.)

        double horizontalDist = flatDistance(prev, player.getLocation());
        double actualSpeed = horizontalDist / elapsedSec;

        double allowedSpeed = BASE_WALK_SPEED;
        if (player.isSprinting()) allowedSpeed *= SPRINT_MULTIPLIER;
        if (player.hasPotionEffect(PotionEffectType.SPEED)) {
            int amplifier = player.getPotionEffect(PotionEffectType.SPEED).getAmplifier();
            allowedSpeed *= 1 + (0.2 * (amplifier + 1));
        }
        allowedSpeed *= LENIENCY_MULTIPLIER;

        if (actualSpeed > allowedSpeed) {
            double overBy = actualSpeed - allowedSpeed;
            double points = Math.min(overBy, 3.0);
            double total = ViolationManager.addViolation(player, ViolationManager.CheckType.SPEED, points);
            if (total >= FLAG_THRESHOLD) {
                alertStaff(player, actualSpeed, allowedSpeed, total);
            }
        }
    }

    private double flatDistance(org.bukkit.Location a, org.bukkit.Location b) {
        double dx = a.getX() - b.getX();
        double dz = a.getZ() - b.getZ();
        return Math.sqrt(dx * dx + dz * dz);
    }

    private void alertStaff(Player player, double actual, double allowed, double total) {
        String msg = String.format("§c[AC] §f%s §7flagged for §cSPEED §7(%.2f b/s vs %.2f allowed, score: %.1f) §8[low-confidence]",
                player.getName(), actual, allowed, total);
        for (Player staff : Bukkit.getOnlinePlayers()) {
            if (staff.hasPermission("arena.anticheat.alerts")) {
                staff.sendMessage(msg);
            }
        }
        Bukkit.getLogger().warning("[AntiCheat] " + player.getName() + " flagged for SPEED (low-confidence): "
                + actual + " b/s vs " + allowed + " allowed");
    }
}
