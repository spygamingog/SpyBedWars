package com.spygamingog.spybedwars.managers;

import com.spygamingog.spybedwars.SpyBedWars;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class LobbyItemManager {

    private final ItemStack leaveGame;

    public LobbyItemManager() {
        this.leaveGame = createItem(Material.RED_BED, ChatColor.RED + "Leave Game " + ChatColor.GRAY + "(Right Click)");
    }

    private ItemStack createItem(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            item.setItemMeta(meta);
        }
        return item;
    }

    public void giveLobbyItems(Player player) {
        player.getInventory().clear();
        // Add more lobby items here if needed
    }

    public void giveWaitingItems(Player player) {
        player.getInventory().clear();
        player.getInventory().setItem(4, com.spygamingog.spybedwars.arena.TeamSelector.getTeamSelectorItem());
        player.getInventory().setItem(8, leaveGame);
    }

    public boolean isLeaveGame(ItemStack item) {
        return isSimilar(item, leaveGame);
    }

    private boolean isSimilar(ItemStack item, ItemStack target) {
        if (item == null || item.getType() == Material.AIR) return false;
        if (item.getType() != target.getType()) return false;
        if (!item.hasItemMeta() || !target.hasItemMeta()) return false;
        return item.getItemMeta().getDisplayName().equals(target.getItemMeta().getDisplayName());
    }
}
