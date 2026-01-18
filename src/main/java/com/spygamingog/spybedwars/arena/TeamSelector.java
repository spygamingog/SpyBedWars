package com.spygamingog.spybedwars.arena;

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

public class TeamSelector {

    public static void open(Player player, Arena arena) {
        Inventory inv = Bukkit.createInventory(null, 9, "Select a Team");

        for (int i = 0; i < arena.getTeams().size(); i++) {
            Team team = arena.getTeams().get(i);
            ItemStack item = new ItemStack(team.getColor().getMaterial());
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(team.getColor().getChatColor() + team.getName());
            
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Players: " + ChatColor.AQUA + team.getMembers().size() + "/" + arena.getArenaType().getTeamSize());
            lore.add("");
            lore.add(ChatColor.YELLOW + "Click to join!");
            
            meta.setLore(lore);
            item.setItemMeta(meta);
            inv.setItem(i, item);
        }

        player.openInventory(inv);
    }

    public static ItemStack getTeamSelectorItem() {
        ItemStack item = new ItemStack(Material.WHITE_WOOL);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.AQUA + "Select Team " + ChatColor.GRAY + "(Right Click)");
            item.setItemMeta(meta);
        }
        return item;
    }
}
