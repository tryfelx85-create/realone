package com.arena.spawn;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionType;

public class Kits {

    public static void clearPlayer(Player player) {
        PlayerInventory inv = player.getInventory();
        inv.clear();
        inv.setHelmet(null);
        inv.setChestplate(null);
        inv.setLeggings(null);
        inv.setBoots(null);
        inv.setItemInOffHand(null);
    }

    private static void giveDiamondArmor(Player player) {
        PlayerInventory inv = player.getInventory();
        inv.setHelmet(new ItemStack(Material.DIAMOND_HELMET));
        inv.setChestplate(new ItemStack(Material.DIAMOND_CHESTPLATE));
        inv.setLeggings(new ItemStack(Material.DIAMOND_LEGGINGS));
        inv.setBoots(new ItemStack(Material.DIAMOND_BOOTS));
    }

    // Kit 1: Diamond armor + diamond sword + 6 steaks
    public static void giveKit1(Player player) {
        clearPlayer(player);
        giveDiamondArmor(player);
        PlayerInventory inv = player.getInventory();
        inv.addItem(new ItemStack(Material.DIAMOND_SWORD));
        inv.addItem(new ItemStack(Material.COOKED_BEEF, 6));
    }

    // Kit 2: Diamond armor + diamond sword + diamond axe + shield + 6 steaks
    public static void giveKit2(Player player) {
        clearPlayer(player);
        giveDiamondArmor(player);
        PlayerInventory inv = player.getInventory();
        inv.addItem(new ItemStack(Material.DIAMOND_SWORD));
        inv.addItem(new ItemStack(Material.DIAMOND_AXE));
        inv.setItemInOffHand(new ItemStack(Material.SHIELD));
        inv.addItem(new ItemStack(Material.COOKED_BEEF, 6));
    }

    // Kit 3: Diamond armor + diamond sword + inventory full of Potion of Healing II
    public static void giveKit3(Player player) {
        clearPlayer(player);
        giveDiamondArmor(player);
        PlayerInventory inv = player.getInventory();

        // Give the sword first and remember its slot so the fill loop doesn't overwrite it
        ItemStack sword = new ItemStack(Material.DIAMOND_SWORD);
        inv.addItem(sword);

        ItemStack healingPotion = new ItemStack(Material.POTION);
        PotionMeta meta = (PotionMeta) healingPotion.getItemMeta();
        meta.setBasePotionType(PotionType.STRONG_HEALING); // Potion of Healing II
        healingPotion.setItemMeta(meta);

        // FIX: fill ALL main-inventory slots (0-35), not just 1-35, so slot 0
        // isn't silently left empty. Since giveDiamondArmor/addItem only ever
        // occupy armor slots + wherever the sword landed, this now correctly
        // fills every remaining empty slot including slot 0.
        for (int slot = 0; slot < 36; slot++) {
            if (inv.getItem(slot) == null) {
                inv.setItem(slot, healingPotion.clone());
            }
        }
    }

    public static void giveKitByChoice(Player player, int kitChoice) {
        switch (kitChoice) {
            case 1 -> giveKit1(player);
            case 2 -> giveKit2(player);
            case 3 -> giveKit3(player);
        }
    }
}
