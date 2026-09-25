package com.arena.anticheat;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Flags autoclickers/killaura-style attack patterns by looking at both:
 *  1. Raw CPS (clicks per second) — very high sustained CPS is rare legit.
 *  2. Timing CONSISTENCY — real humans have irregular intervals between
 *     clicks; many autoclickers fire at suspiciously uniform intervals
 *     (low variance). This is the stronger signal of the two, since some
 *     players genuinely can click fast, but doing so with near-zero
 *     variance for many hits in a row is very hard to fake by hand.
 *
 * CAVEAT: skilled "jitter-clicker" or "butterfly-clicker" humans can
 * produce high CPS with irregular timing and won't be flagged here — this
 * check targets the more common uniform-interval autoclicker pattern.
 */
public class CPSCheck implements Listener {

    private static final int WINDOW_SIZE = 10;           // hits to analyze
    private static final double HIGH_CPS_THRESHOLD = 14;  // sustained CPS considered suspicious
    private static final double LOW_VARIANCE_MS = 8.0;    // stddev below this = suspiciously uniform
    private static final double FLAG_THRESHOLD = 6.0;

    private final Map<UUID, Deque<Long>> hitTimestamps = new ConcurrentHashMap<>();

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker)) return;
        if (!(event.getEntity() instanceof Player)) return;

        long now = System.currentTimeMillis();
        Deque<Long> timestamps = hitTimestamps.computeIfAbsent(attacker.getUniqueId(), k -> new ArrayDeque<>());
        timestamps.addLast(now);
        while (timestamps.size() > WINDOW_SIZE) {
            timestamps.removeFirst();
        }

        if (timestamps.size() < WINDOW_SIZE) return; // not enough data yet

        Long[] times = timestamps.toArray(new Long[0]);
        double[] intervals = new double[times.length - 1];
        for (int i = 1; i < times.length; i++) {
            intervals[i - 1] = times[i] - times[i - 1];
        }

        double meanInterval = mean(intervals);
        double stddev = stddev(intervals, meanInterval);
        double cps = meanInterval > 0 ? 1000.0 / meanInterval : 0;

        boolean suspicious = false;
        double points = 0;

        if (cps > HIGH_CPS_THRESHOLD && stddev < LOW_VARIANCE_MS) {
            // Both high speed AND robotic consistency — strong signal
            suspicious = true;
            points = 4.0;
        } else if (stddev < LOW_VARIANCE_MS / 2 && cps > 8) {
            // Extremely uniform timing even at moderate CPS is still unusual
            suspicious = true;
            points = 2.0;
        }

        if (suspicious) {
            double total = ViolationManager.addViolation(attacker, ViolationManager.CheckType.CPS, points);
            if (total >= FLAG_THRESHOLD) {
                alertStaff(attacker, cps, stddev, total);
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

    private void alertStaff(Player attacker, double cps, double stddev, double total) {
        String msg = String.format("§c[AC] §f%s §7flagged for §cCPS/AUTOCLICK §7(%.1f cps, stddev %.1fms, score: %.1f)",
                attacker.getName(), cps, stddev, total);
        for (Player staff : Bukkit.getOnlinePlayers()) {
            if (staff.hasPermission("arena.anticheat.alerts")) {
                staff.sendMessage(msg);
            }
        }
        Bukkit.getLogger().warning("[AntiCheat] " + attacker.getName() + " flagged for CPS/AUTOCLICK: "
                + cps + " cps, stddev " + stddev + "ms");
    }
}
