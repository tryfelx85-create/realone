package com.arena.spawn;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public class VoteListener implements Listener {

    @EventHandler
    public void onVoteClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof VoteInventoryHolder)) return;

        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) return;

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null) return;

        int kitChoice;
        switch (clicked.getType()) {
            case DIAMOND_SWORD -> kitChoice = 1;
            case DIAMOND_AXE -> kitChoice = 2;
            case POTION -> kitChoice = 3;
            default -> {
                return;
            }
        }

        VoteManager.castVote(player, kitChoice);
        player.closeInventory();
        player.sendMessage("§7Vote registered. Waiting for the match to start...");
    }
}
