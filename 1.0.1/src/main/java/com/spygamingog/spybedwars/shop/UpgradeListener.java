package com.spygamingog.spybedwars.shop;

import com.spygamingog.spybedwars.SpyBedWars;
import com.spygamingog.spybedwars.arena.Arena;
import com.spygamingog.spybedwars.arena.team.Team;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Optional;

public class UpgradeListener implements Listener {

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!event.getView().getTitle().equals("Team Upgrades")) return;

        event.setCancelled(true);
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR || clicked.getType() == Material.GRAY_STAINED_GLASS_PANE) return;

        Optional<Arena> arenaOpt = SpyBedWars.getInstance().getArenaManager().getArenaByPlayer(player);
        if (!arenaOpt.isPresent()) return;

        Arena arena = arenaOpt.get();
        Team team = arena.getTeams().stream().filter(t -> t.getMembers().contains(player.getUniqueId())).findFirst().orElse(null);
        if (team == null) return;

        int slot = event.getSlot();
        switch (slot) {
            case 10 -> handleUpgrade(player, team, "sharpness", new int[]{4});
            case 11 -> handleUpgrade(player, team, "protection", new int[]{2, 4, 8, 16});
            case 12 -> handleUpgrade(player, team, "haste", new int[]{2, 4});
            case 13 -> handleUpgrade(player, team, "forge", new int[]{2, 4, 6, 8});
            case 14 -> handleUpgrade(player, team, "heal_pool", new int[]{1});
        }
    }

    private void handleUpgrade(Player player, Team team, String key, int[] costs) {
        int level = team.getUpgrades().getOrDefault(key, 0);
        if (level >= costs.length) {
            TextComponent msg = new TextComponent(ChatColor.RED + "This upgrade is already maxed!");
            msg.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(ChatColor.GRAY + "No further levels available.")));
            msg.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/bw help"));
            player.spigot().sendMessage(msg);
            return;
        }

        int cost = costs[level];
        if (hasDiamonds(player, cost)) {
            removeDiamonds(player, cost);
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
            team.getUpgrades().put(key, level + 1);
            applyUpgrades(team, key, level + 1);
            TextComponent msg = new TextComponent(ChatColor.GREEN + "Upgrade purchased!");
            msg.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(ChatColor.GRAY + "Team benefit unlocked.")));
            msg.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/bw leave"));
            player.spigot().sendMessage(msg);
            SpyBedWars.getInstance().getUpgradeManager().openUpgrades(player, team);
        } else {
            TextComponent msg = new TextComponent(ChatColor.RED + "You don't have enough diamonds!");
            msg.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(ChatColor.GRAY + "Gather more diamonds from generators.")));
            msg.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/bw help"));
            player.spigot().sendMessage(msg);
        }
    }

    private boolean hasDiamonds(Player player, int amount) {
        int count = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == Material.DIAMOND) {
                count += item.getAmount();
            }
        }
        return count >= amount;
    }

    private void removeDiamonds(Player player, int amount) {
        int remaining = amount;
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            if (item != null && item.getType() == Material.DIAMOND) {
                if (item.getAmount() <= remaining) {
                    remaining -= item.getAmount();
                    player.getInventory().setItem(i, null);
                } else {
                    item.setAmount(item.getAmount() - remaining);
                    remaining = 0;
                }
            }
            if (remaining <= 0) break;
        }
    }

    private void applyUpgrades(Team team, String key, int level) {
        for (java.util.UUID uuid : team.getMembers()) {
            Player p = org.bukkit.Bukkit.getPlayer(uuid);
            if (p == null) continue;

            switch (key) {
                case "sharpness" -> {
                    for (ItemStack item : p.getInventory().getContents()) {
                        if (item != null && item.getType().name().contains("SWORD")) {
                            item.addUnsafeEnchantment(Enchantment.SHARPNESS, 1);
                        }
                    }
                }
                case "protection" -> {
                    for (ItemStack item : p.getInventory().getArmorContents()) {
                        if (item != null && !item.getType().isAir()) {
                            item.addUnsafeEnchantment(Enchantment.PROTECTION, level);
                        }
                    }
                }
                case "haste" -> {
                    p.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, Integer.MAX_VALUE, level - 1));
                }
            }
        }
    }
}
