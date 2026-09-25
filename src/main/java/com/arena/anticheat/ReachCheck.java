package com.arena.anticheat;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

/**
 * Flags melee hits landed from beyond legitimate reach.
 * Vanilla survival reach is ~3.0 blocks; Paper's own server-side check
 * already blocks the most blatant cases, so this catches borderline
 * "3.1-4.5 block" reach cheats that slip through, without false-flagging
 * normal lag/latency (small buffer included for that).
 *
 * CAVEAT: this uses Bukkit's post-processed location data, not raw
 * packets. A cheat that also fakes position data server-side (rare, but
 * exists) won't be caught here — that requires packet-level inspection
 * (e.g. via ProtocolLib), which this does not use.
 */
public class ReachCheck implements Listener {

    private static final double MAX_LEGIT_REACH = 3.6; // vanilla ~3.0 + latency/lag buffer
    private static final double FLAG_THRESHOLD = 6.0;   // violation points before staff alert

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;
        if (!(event.getDamager() instanceof Player attacker)) return;

        double distance = attacker.getLocation().distance(victim.getLocation());

        if (distance > MAX_LEGIT_REACH) {
            // Scale violation severity by how far over the limit they are
            double overBy = distance - MAX_LEGIT_REACH;
            double points = Math.min(overBy * 2, 5.0); // cap a single hit's contribution

            double total = ViolationManager.addViolation(attacker, ViolationManager.CheckType.REACH, points);

            if (total >= FLAG_THRESHOLD) {
                alertStaff(attacker, distance, total);
            }
        }
    }

    private void alertStaff(Player attacker, double distance, double total) {
        String msg = String.format("§c[AC] §f%s §7flagged for §cREACH §7(%.2f blocks, score: %.1f)",
                attacker.getName(), distance, total);
        for (Player staff : Bukkit.getOnlinePlayers()) {
            if (staff.hasPermission("arena.anticheat.alerts")) {
                staff.sendMessage(msg);
            }
        }
        Bukkit.getLogger().warning("[AntiCheat] " + attacker.getName() + " flagged for REACH: " + distance + " blocks");
    }
}
