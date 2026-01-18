package com.spygamingog.spybedwars.shop;

import com.spygamingog.spybedwars.SpyBedWars;
import com.spygamingog.spybedwars.arena.team.Team;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class UpgradeManager {

    public void openUpgrades(Player player, Team team) {
        Inventory inv = Bukkit.createInventory(null, 27, "Team Upgrades");

        // Background
        ItemStack glass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta glassMeta = glass.getItemMeta();
        glassMeta.setDisplayName(" ");
        glass.setItemMeta(glassMeta);
        for (int i = 0; i < 27; i++) inv.setItem(i, glass);

        // Sharpness
        addUpgradeItem(inv, 10, "Sharpened Swords", Material.IRON_SWORD, "sharpness", team, 4);
        // Protection
        addUpgradeItem(inv, 11, "Reinforced Armor", Material.IRON_CHESTPLATE, "protection", team, 2, 4, 8, 16);
        // Haste
        addUpgradeItem(inv, 12, "Maniac Miner", Material.GOLDEN_PICKAXE, "haste", team, 2, 4);
        // Forge
        addUpgradeItem(inv, 13, "Iron Forge", Material.FURNACE, "forge", team, 2, 4, 6, 8);
        // Heal Pool
        addUpgradeItem(inv, 14, "Heal Pool", Material.BEACON, "heal_pool", team, 1);

        player.openInventory(inv);
    }

    private void addUpgradeItem(Inventory inv, int slot, String name, Material icon, String key, Team team, int... costs) {
        int level = team.getUpgrades().getOrDefault(key, 0);
        boolean maxed = level >= costs.length;
        
        ItemStack item = new ItemStack(icon);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.GREEN + name + (maxed ? ChatColor.GOLD + " [MAX]" : ""));
        
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Level: " + ChatColor.AQUA + level + "/" + costs.length);
        lore.add("");
        
        if (!maxed) {
            lore.add(ChatColor.GRAY + "Cost: " + ChatColor.DARK_AQUA + costs[level] + " Diamonds");
            lore.add("");
            lore.add(ChatColor.YELLOW + "Click to upgrade!");
        } else {
            lore.add(ChatColor.RED + "Maximum level reached!");
        }
        
        meta.setLore(lore);
        item.setItemMeta(meta);
        inv.setItem(slot, item);
    }
}
