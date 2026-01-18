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
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Optional;

public class ShopListener implements Listener {

    private final java.util.Map<java.util.UUID, String> pendingQuickBuy = new java.util.HashMap<>();

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        
        String title = event.getView().getTitle();
        if (!title.startsWith("Shop - ")) return;
        
        event.setCancelled(true);
        
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;
        
        ShopManager shopManager = SpyBedWars.getInstance().getShopManager();
        String categoryName = title.replace("Shop - ", "");
        ShopCategory currentCategory = null;
        for (ShopCategory cat : ShopCategory.values()) {
            if (cat.getDisplayName().equals(categoryName)) {
                currentCategory = cat;
                break;
            }
        }
        
        if (currentCategory == null) return;

        // Check for category switching
        for (ShopCategory cat : ShopCategory.values()) {
            if (event.getSlot() == cat.getSlot()) {
                shopManager.openShop(player, cat);
                return;
            }
        }

        // Handle item purchase
        ShopItem item = shopManager.getItemAtSlot(player, currentCategory, event.getSlot());
        
        // Handle Quick Buy customization (Shift+Click)
        if (event.isShiftClick()) {
            if (currentCategory != ShopCategory.QUICK_BUY && item != null) {
                player.sendMessage(ChatColor.YELLOW + "Added " + item.getName() + " to Quick Buy!");
                player.sendMessage(ChatColor.GRAY + "Select a slot in the Quick Buy menu to place it.");
                // Store the item the player wants to add
                pendingQuickBuy.put(player.getUniqueId(), item.getName());
                shopManager.openShop(player, ShopCategory.QUICK_BUY);
                return;
            } else if (currentCategory == ShopCategory.QUICK_BUY && item != null) {
                // Remove from Quick Buy
                SpyBedWars.getInstance().getQuickBuyManager().removeQuickBuyItem(player, event.getSlot());
                player.sendMessage(ChatColor.RED + "Removed item from Quick Buy!");
                shopManager.openShop(player, ShopCategory.QUICK_BUY);
                return;
            }
        }

        // Handle Quick Buy placement
        if (currentCategory == ShopCategory.QUICK_BUY && pendingQuickBuy.containsKey(player.getUniqueId())) {
            int slot = event.getSlot();
            // Restrict to valid Quick Buy slots (3 rows: 19-25, 28-34, 37-43)
            boolean isValidSlot = (slot >= 19 && slot <= 25) || (slot >= 28 && slot <= 34) || (slot >= 37 && slot <= 43);
            
            if (isValidSlot) {
                String itemName = pendingQuickBuy.remove(player.getUniqueId());
                SpyBedWars.getInstance().getQuickBuyManager().setQuickBuyItem(player, slot, itemName);
                player.sendMessage(ChatColor.GREEN + "Placed " + itemName + " in Quick Buy slot!");
                shopManager.openShop(player, ShopCategory.QUICK_BUY);
            } else {
                player.sendMessage(ChatColor.RED + "You cannot place items in this slot!");
            }
            return;
        }

        if (item != null) {
            Optional<Arena> arenaOpt = SpyBedWars.getInstance().getArenaManager().getArenaByPlayer(player);
            if (!arenaOpt.isPresent()) return;
            Arena arena = arenaOpt.get();

            Material currency = item.getCurrency();
            int price = item.getPrice();

            // Adjust prices for TNT and Bridge Egg based on ArenaType
            if (item.getName().equalsIgnoreCase("TNT")) {
                if (arena.getArenaType().name().contains("SOLO") || arena.getArenaType().name().contains("DOUBLE")) {
                    price = 5;
                } else {
                    price = 10;
                }
            } else if (item.getName().equalsIgnoreCase("Bridge Egg")) {
                if (arena.getArenaType().name().contains("SOLO") || arena.getArenaType().name().contains("DOUBLE")) {
                    price = 2;
                } else {
                    price = 4;
                }
            }

            // Handle Tools
            if (item.getName().contains("Pickaxe Upgrade")) {
                handleToolUpgrade(player, arena, "pickaxe", currency, price);
                return;
            } else if (item.getName().contains("Axe Upgrade")) {
                handleToolUpgrade(player, arena, "axe", currency, price);
                return;
            } else if (item.getName().equalsIgnoreCase("Shears")) {
                if (arena.getHasShears().getOrDefault(player.getUniqueId(), false)) {
                    TextComponent msg = new TextComponent(ChatColor.RED + "You already have shears!");
                    msg.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(ChatColor.GRAY + "You cannot buy shears again.")));
                    msg.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/bw help"));
                    player.spigot().sendMessage(msg);
                    return;
                }
                if (purchase(player, currency, price)) {
                    arena.getHasShears().put(player.getUniqueId(), true);
                    player.getInventory().addItem(new ItemStack(Material.SHEARS));
                    
                    player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
                    
                    TextComponent msg = new TextComponent(ChatColor.GREEN + "Purchased Shears!");
                    msg.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(ChatColor.GRAY + "Enjoy your new tool!")));
                    msg.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/bw leave"));
                    player.spigot().sendMessage(msg);
                }
                return;
            }

            // Handle permanent armor
            if (item.getName().contains("Armor")) {
                    int level = 0;
                    Material type = item.getItemStack().getType();
                    if (type == Material.CHAINMAIL_BOOTS) level = 1;
                    else if (type == Material.IRON_BOOTS) level = 2;
                    else if (type == Material.DIAMOND_BOOTS) level = 3;
                    
                    int currentLevel = arena.getArmorLevels().getOrDefault(player.getUniqueId(), 0);
                    if (level <= currentLevel) {
                        TextComponent msg = new TextComponent(ChatColor.RED + "You already have this armor or better!");
                        msg.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(ChatColor.GRAY + "No need to downgrade!")));
                        msg.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/bw help"));
                        player.spigot().sendMessage(msg);
                        return;
                    }
                    
                    if (purchase(player, currency, price)) {
                        arena.getArmorLevels().put(player.getUniqueId(), level);
                        applyArmor(player, level);
                        TextComponent msg = new TextComponent(ChatColor.GREEN + "Purchased " + item.getName() + "!");
                        msg.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(ChatColor.GRAY + "Armor automatically applied.")));
                        msg.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/bw leave"));
                        player.spigot().sendMessage(msg);
                    }
                    return;
                }

                if (purchase(player, currency, price)) {
                    ItemStack toAdd = item.getItemStack().clone();
                
                // Handle colored blocks
                if (toAdd.getType().name().contains("WOOL") || toAdd.getType() == Material.TERRACOTTA || toAdd.getType() == Material.GLASS) {
                    Team team = arena.getTeam(player);
                    if (team != null) {
                        if (toAdd.getType().name().contains("WOOL")) toAdd.setType(team.getColor().getWool());
                        else if (toAdd.getType() == Material.TERRACOTTA) toAdd.setType(team.getColor().getTerracotta());
                        else if (toAdd.getType() == Material.GLASS) toAdd.setType(team.getColor().getGlass());
                    }
                }

                // Custom logic for wooden sword (FREE)
                if (item.getName().equalsIgnoreCase("Wooden Sword")) {
                    // Just add it if they don't have it
                    if (!player.getInventory().contains(Material.WOODEN_SWORD)) {
                        player.getInventory().addItem(toAdd);
                    }
                } else {
                    player.getInventory().addItem(toAdd);
                }
                
                TextComponent msg = new TextComponent(ChatColor.GREEN + "Purchased " + item.getName() + "!");
                msg.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(ChatColor.GRAY + "Item added to inventory.")));
                msg.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/bw leave"));
                player.spigot().sendMessage(msg);
            }
        }
    }

    private boolean purchase(Player player, Material currency, int price) {
        if (hasEnoughResources(player, currency, price)) {
            removeResources(player, currency, price);
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
            return true;
        }
        TextComponent msg = new TextComponent(ChatColor.RED + "You don't have enough " + currency.name().replace("_INGOT", "") + "!");
        msg.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(ChatColor.GRAY + "Gather more resources to buy this.")));
        msg.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/bw help"));
        player.spigot().sendMessage(msg);
        return false;
    }

    private void handleToolUpgrade(Player player, Arena arena, String toolType, Material currency, int price) {
        int currentLevel = toolType.equals("pickaxe") ? 
            arena.getPickaxeLevels().getOrDefault(player.getUniqueId(), 0) : 
            arena.getAxeLevels().getOrDefault(player.getUniqueId(), 0);

        if (currentLevel >= 4) {
            TextComponent msg = new TextComponent(ChatColor.RED + "You already have the maximum upgrade!");
            msg.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(ChatColor.GRAY + "No further upgrades available.")));
            msg.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/bw leave"));
            player.spigot().sendMessage(msg);
            return;
        }

        // Determine next level cost and material
        Material nextCurrency = Material.IRON_INGOT;
        int nextPrice = 10;
        Material toolMat = Material.WOODEN_PICKAXE;
        int effLevel = 1;

        if (toolType.equals("pickaxe")) {
            switch (currentLevel) {
                case 0 -> { nextCurrency = Material.IRON_INGOT; nextPrice = 10; toolMat = Material.WOODEN_PICKAXE; effLevel = 1; }
                case 1 -> { nextCurrency = Material.IRON_INGOT; nextPrice = 10; toolMat = Material.STONE_PICKAXE; effLevel = 1; }
                case 2 -> { nextCurrency = Material.GOLD_INGOT; nextPrice = 3; toolMat = Material.IRON_PICKAXE; effLevel = 2; }
                case 3 -> { nextCurrency = Material.GOLD_INGOT; nextPrice = 6; toolMat = Material.DIAMOND_PICKAXE; effLevel = 3; }
            }
        } else {
            switch (currentLevel) {
                case 0 -> { nextCurrency = Material.IRON_INGOT; nextPrice = 10; toolMat = Material.WOODEN_AXE; effLevel = 1; }
                case 1 -> { nextCurrency = Material.IRON_INGOT; nextPrice = 10; toolMat = Material.STONE_AXE; effLevel = 1; }
                case 2 -> { nextCurrency = Material.GOLD_INGOT; nextPrice = 3; toolMat = Material.IRON_AXE; effLevel = 2; }
                case 3 -> { nextCurrency = Material.GOLD_INGOT; nextPrice = 6; toolMat = Material.DIAMOND_AXE; effLevel = 3; }
            }
        }

        if (purchase(player, nextCurrency, nextPrice)) {
            int newLevel = currentLevel + 1;
            if (toolType.equals("pickaxe")) arena.getPickaxeLevels().put(player.getUniqueId(), newLevel);
            else arena.getAxeLevels().put(player.getUniqueId(), newLevel);

            // Remove old tool
            player.getInventory().remove(Material.WOODEN_PICKAXE);
            player.getInventory().remove(Material.STONE_PICKAXE);
            player.getInventory().remove(Material.IRON_PICKAXE);
            player.getInventory().remove(Material.DIAMOND_PICKAXE);
            player.getInventory().remove(Material.WOODEN_AXE);
            player.getInventory().remove(Material.STONE_AXE);
            player.getInventory().remove(Material.IRON_AXE);
            player.getInventory().remove(Material.DIAMOND_AXE);

            ItemStack tool = new ItemStack(toolMat);
            ItemMeta meta = tool.getItemMeta();
            meta.addEnchant(org.bukkit.enchantments.Enchantment.EFFICIENCY, effLevel, true);
            tool.setItemMeta(meta);
            player.getInventory().addItem(tool);

            TextComponent msg = new TextComponent(ChatColor.GREEN + "Upgraded " + toolType + "!");
            msg.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(ChatColor.GRAY + "New tool added to inventory.")));
            msg.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/bw leave"));
            player.spigot().sendMessage(msg);
        }
    }

    private void applyArmor(Player player, int level) {
        Optional<Arena> arenaOpt = SpyBedWars.getInstance().getArenaManager().getArenaByPlayer(player);
        if (!arenaOpt.isPresent()) return;
        Team team = arenaOpt.get().getTeam(player);
        if (team == null) return;
        org.bukkit.Color color = team.getColor().getColor();

        ItemStack helmet = new ItemStack(Material.LEATHER_HELMET);
        ItemStack chestplate = new ItemStack(Material.LEATHER_CHESTPLATE);
        
        org.bukkit.inventory.meta.LeatherArmorMeta meta = (org.bukkit.inventory.meta.LeatherArmorMeta) helmet.getItemMeta();
        meta.setColor(color);
        helmet.setItemMeta(meta);
        
        meta = (org.bukkit.inventory.meta.LeatherArmorMeta) chestplate.getItemMeta();
        meta.setColor(color);
        chestplate.setItemMeta(meta);

        player.getInventory().setHelmet(helmet);
        player.getInventory().setChestplate(chestplate);

        Material boots = Material.LEATHER_BOOTS;
        Material leggings = Material.LEATHER_LEGGINGS;
        
        switch (level) {
            case 1 -> { boots = Material.CHAINMAIL_BOOTS; leggings = Material.CHAINMAIL_LEGGINGS; }
            case 2 -> { boots = Material.IRON_BOOTS; leggings = Material.IRON_LEGGINGS; }
            case 3 -> { boots = Material.DIAMOND_BOOTS; leggings = Material.DIAMOND_LEGGINGS; }
        }
        
        ItemStack bootsItem = new ItemStack(boots);
        ItemStack leggingsItem = new ItemStack(leggings);
        
        if (boots == Material.LEATHER_BOOTS) {
            meta = (org.bukkit.inventory.meta.LeatherArmorMeta) bootsItem.getItemMeta();
            meta.setColor(color);
            bootsItem.setItemMeta(meta);
        }
        if (leggings == Material.LEATHER_LEGGINGS) {
            meta = (org.bukkit.inventory.meta.LeatherArmorMeta) leggingsItem.getItemMeta();
            meta.setColor(color);
            leggingsItem.setItemMeta(meta);
        }

        player.getInventory().setBoots(bootsItem);
        player.getInventory().setLeggings(leggingsItem);
    }

    private boolean hasEnoughResources(Player player, Material material, int amount) {
        int count = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == material) {
                count += item.getAmount();
            }
        }
        return count >= amount;
    }

    private void removeResources(Player player, Material material, int amount) {
        int remaining = amount;
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            if (item != null && item.getType() == material) {
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
}
