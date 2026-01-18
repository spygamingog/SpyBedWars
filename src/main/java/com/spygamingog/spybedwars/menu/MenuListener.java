package com.spygamingog.spybedwars.menu;

import com.spygamingog.spybedwars.SpyBedWars;
import com.spygamingog.spybedwars.api.arena.ArenaType;
import com.spygamingog.spybedwars.arena.Arena;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;

public class MenuListener implements Listener {

    private final SpyBedWars plugin;

    public MenuListener(SpyBedWars plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        String title = event.getView().getTitle();
        if (!title.startsWith(ChatColor.DARK_GRAY + "BedWars - ")) return;

        event.setCancelled(true);

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR || clicked.getType() == Material.GRAY_STAINED_GLASS_PANE) return;

        MenuManager menuManager = plugin.getMenuManager();

        if (title.equals(ChatColor.DARK_GRAY + "BedWars - Modes")) {
            handleMainMenu(player, clicked, menuManager);
        } else {
            handleModeMenu(player, clicked, title.replace(ChatColor.DARK_GRAY + "BedWars - ", ""), menuManager);
        }
    }

    private void handleMainMenu(Player player, ItemStack clicked, MenuManager menuManager) {
        String name = ChatColor.stripColor(clicked.getItemMeta().getDisplayName());
        try {
            ArenaType type = ArenaType.valueOf(name);
            menuManager.openModeMenu(player, type);
        } catch (IllegalArgumentException ignored) {}
    }

    private void handleModeMenu(Player player, ItemStack clicked, String modeName, MenuManager menuManager) {
        if (clicked.getType() == Material.ARROW) {
            menuManager.openMainMenu(player);
            return;
        }

        String arenaName = ChatColor.stripColor(clicked.getItemMeta().getDisplayName());
        Optional<Arena> arenaOpt = plugin.getArenaManager().getArenaByName(arenaName);
        
        if (arenaOpt.isPresent()) {
            Arena arena = arenaOpt.get();
            if (arena.getPlayers().size() >= arena.getMaxPlayers()) {
                player.sendMessage(ChatColor.RED + "This arena is full!");
                return;
            }
            
            player.closeInventory();
            player.performCommand("bw join " + arenaName);
        }
    }
}
