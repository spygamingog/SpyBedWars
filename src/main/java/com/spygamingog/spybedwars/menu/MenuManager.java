package com.spygamingog.spybedwars.menu;

import com.spygamingog.spybedwars.SpyBedWars;
import com.spygamingog.spybedwars.api.arena.ArenaType;
import com.spygamingog.spybedwars.arena.Arena;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class MenuManager {

    private final SpyBedWars plugin;

    public MenuManager(SpyBedWars plugin) {
        this.plugin = plugin;
    }

    public void openMainMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, ChatColor.DARK_GRAY + "BedWars - Modes");

        // Fill background
        ItemStack glass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta glassMeta = glass.getItemMeta();
        glassMeta.setDisplayName(" ");
        glass.setItemMeta(glassMeta);
        for (int i = 0; i < 27; i++) {
            inv.setItem(i, glass);
        }

        inv.setItem(10, createModeItem(ArenaType.SOLO, Material.RED_WOOL));
        inv.setItem(12, createModeItem(ArenaType.DOUBLES, Material.GREEN_WOOL));
        inv.setItem(14, createModeItem(ArenaType.TRIPLES, Material.YELLOW_WOOL));
        inv.setItem(16, createModeItem(ArenaType.FOURS, Material.BLUE_WOOL));

        player.openInventory(inv);
    }

    private ItemStack createModeItem(ArenaType type, Material material) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD + "" + ChatColor.BOLD + type.name());
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Click to select a " + type.name().toLowerCase() + " map!");
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    public void openModeMenu(Player player, ArenaType type) {
        Inventory inv = Bukkit.createInventory(null, 54, ChatColor.DARK_GRAY + "BedWars - " + type.name());

        // Fill background
        ItemStack glass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta glassMeta = glass.getItemMeta();
        glassMeta.setDisplayName(" ");
        glass.setItemMeta(glassMeta);
        for (int i = 0; i < 54; i++) {
            if (i < 9 || i >= 45 || i % 9 == 0 || (i + 1) % 9 == 0) {
                inv.setItem(i, glass);
            }
        }

        List<Arena> arenas = plugin.getArenaManager().getArenas().stream()
                .filter(a -> a.getArenaType() == type)
                .collect(Collectors.toList());

        int slot = 10;
        for (Arena arena : arenas) {
            if (slot % 9 == 8) slot += 2; // Skip sides

            ItemStack item = new ItemStack(getWoolColor(type));
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(ChatColor.GREEN + arena.getArenaName());
            
            List<String> lore = new ArrayList<>();
            int currentPlayers = arena.getPlayers().size();
            int maxPlayers = arena.getMaxPlayers();
            
            lore.add(ChatColor.GRAY + "Players: " + ChatColor.YELLOW + currentPlayers + "/" + maxPlayers);
            lore.add("");
            if (currentPlayers >= maxPlayers) {
                lore.add(ChatColor.RED + "This arena is full!");
            } else {
                lore.add(ChatColor.YELLOW + "Click to join!");
            }
            
            meta.setLore(lore);
            item.setItemMeta(meta);
            inv.setItem(slot, item);
            slot++;
            
            if (slot >= 44) break; // Limit to one page for now
        }

        // Back button
        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta backMeta = back.getItemMeta();
        backMeta.setDisplayName(ChatColor.RED + "Back to Modes");
        back.setItemMeta(backMeta);
        inv.setItem(49, back);

        player.openInventory(inv);
    }

    private Material getWoolColor(ArenaType type) {
        switch (type) {
            case SOLO: return Material.RED_WOOL;
            case DOUBLES: return Material.GREEN_WOOL;
            case TRIPLES: return Material.YELLOW_WOOL;
            case FOURS: return Material.BLUE_WOOL;
            default: return Material.WHITE_WOOL;
        }
    }
}
