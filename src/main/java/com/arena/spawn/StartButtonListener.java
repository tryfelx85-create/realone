package com.arena.spawn;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.ArrayList;
import java.util.List;

public class StartButtonListener implements Listener {

    // Fighter spawn points for a 1v1 match
    private static final double P1_X = 70, P1_Y = 5, P1_Z = 33;
    private static final double P2_X = 70, P2_Y = 5, P2_Z = 90;

    // The one button that triggers a match — placed on the west face of the block at 38, 10, 60
    private static final int BUTTON_X = 38, BUTTON_Y = 10, BUTTON_Z = 60;

    private final ArenaPlugin plugin; // FIX: needed to start the vote timer

    public StartButtonListener(ArenaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onButtonPress(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getClickedBlock() == null) return;
        if (!event.getClickedBlock().getType().name().endsWith("_BUTTON")) return;

        // Only react to the designated start button, not every button on the map
        if (event.getClickedBlock().getX() != BUTTON_X
                || event.getClickedBlock().getY() != BUTTON_Y
                || event.getClickedBlock().getZ() != BUTTON_Z) {
            return;
        }

        Player presser = event.getPlayer();
        World world = presser.getWorld();

        // FIX: don't allow starting a new match while one is already active
        if (MatchManager.isMatchActive()) {
            presser.sendMessage("§cA match is already in progress.");
            return;
        }

        // Gather all players currently in Adventure mode (moderators stay in Creative and are ignored)
        List<Player> adventurePlayers = new ArrayList<>();
        for (Player p : world.getPlayers()) {
            if (p.getGameMode() == GameMode.ADVENTURE) {
                adventurePlayers.add(p);
            }
        }

        if (adventurePlayers.size() != 2) {
            presser.sendMessage("§cA match needs exactly 2 players in Adventure mode. Currently: "
                    + adventurePlayers.size());
            return;
        }

        Player player1 = adventurePlayers.get(0);
        Player player2 = adventurePlayers.get(1);

        player1.teleport(new Location(world, P1_X, P1_Y, P1_Z));
        player2.teleport(new Location(world, P2_X, P2_Y, P2_Z));

        MatchManager.startMatch(player1.getUniqueId(), player2.getUniqueId());
        VoteManager.reset();
        VoteManager.startTimer(plugin); // FIX: actually start the 30s countdown

        player1.sendMessage("§aMatch started! Choose your kit.");
        player2.sendMessage("§aMatch started! Choose your kit.");

        VoteGUI.open(player1);
        VoteGUI.open(player2);

        for (Player p : world.getPlayers()) {
            if (!p.equals(player1) && !p.equals(player2)) {
                p.sendMessage("§eA match has started between §f" + player1.getName()
                        + "§e and §f" + player2.getName() + "§e.");
            }
        }
    }
}
