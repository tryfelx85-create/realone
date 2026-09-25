package com.arena.spawn;

import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;

/**
 * FIX for missing match-end logic: without this, MatchManager.endMatch()
 * was never called anywhere, so isFighting() stayed true for the two
 * fighters forever after a match concluded.
 */
public class MatchEndListener implements Listener {

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player loser = event.getEntity();
        UUID loserId = loser.getUniqueId();

        if (!MatchManager.isFighting(loserId)) return;

        UUID p1 = MatchManager.getPlayer1();
        UUID p2 = MatchManager.getPlayer2();
        UUID winnerId = loserId.equals(p1) ? p2 : p1;

        Player winner = winnerId != null ? org.bukkit.Bukkit.getPlayer(winnerId) : null;

        if (winner != null) {
            winner.sendMessage("§a§lYou won the match!");
        }
        loser.sendMessage("§c§lYou lost the match.");

        MatchManager.endMatch();
        VoteManager.reset();

        // Return both players to Adventure mode / spawn so they're ready for the next match
        if (winner != null) {
            winner.setGameMode(GameMode.ADVENTURE);
            winner.teleport(loser.getWorld().getSpawnLocation());
        }
        // Note: the loser's own respawn is handled by PlayerRespawnEvent /
        // server config; we just make sure the match state is cleared here.
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID quitterId = event.getPlayer().getUniqueId();

        // FIX (bonus): if a fighter disconnects mid-match, don't leave the
        // match stuck open forever with a permanently "fighting" opponent.
        if (!MatchManager.isFighting(quitterId)) return;

        UUID p1 = MatchManager.getPlayer1();
        UUID p2 = MatchManager.getPlayer2();
        UUID remainingId = quitterId.equals(p1) ? p2 : p1;

        Player remaining = remainingId != null ? org.bukkit.Bukkit.getPlayer(remainingId) : null;
        if (remaining != null) {
            remaining.sendMessage("§eYour opponent disconnected. Match ended.");
            remaining.setGameMode(GameMode.ADVENTURE);
            remaining.teleport(remaining.getWorld().getSpawnLocation());
        }

        MatchManager.endMatch();
        VoteManager.reset();
    }
}
