package com.spygamingog.spybedwars.shop;

import com.spygamingog.spybedwars.SpyBedWars;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ShopManager {

    private final Map<ShopCategory, Map<Integer, ShopItem>> shopItems = new HashMap<>();

    public ShopManager() {
        loadDefaultItems();
    }

    private void loadDefaultItems() {
        // Blocks (Category 0)
        addShopItem(20, new ShopItem("Wool", new ItemStack(Material.WHITE_WOOL, 16), Material.IRON_INGOT, 4, ShopCategory.BLOCKS));
        addShopItem(21, new ShopItem("Hardened Clay", new ItemStack(Material.TERRACOTTA, 16), Material.IRON_INGOT, 12, ShopCategory.BLOCKS));
        addShopItem(22, new ShopItem("Blast Proof Glass", new ItemStack(Material.GLASS, 12), Material.IRON_INGOT, 12, ShopCategory.BLOCKS));
        addShopItem(23, new ShopItem("End Stone", new ItemStack(Material.END_STONE, 12), Material.IRON_INGOT, 24, ShopCategory.BLOCKS));
        addShopItem(24, new ShopItem("Oak Planks", new ItemStack(Material.OAK_PLANKS, 16), Material.GOLD_INGOT, 4, ShopCategory.BLOCKS));
        addShopItem(29, new ShopItem("Obsidian", new ItemStack(Material.OBSIDIAN, 1), Material.EMERALD, 4, ShopCategory.BLOCKS));
        addShopItem(30, new ShopItem("Ladder", new ItemStack(Material.LADDER, 8), Material.IRON_INGOT, 4, ShopCategory.BLOCKS));

        // Melee (Category 1)
        addShopItem(20, new ShopItem("Wooden Sword", new ItemStack(Material.WOODEN_SWORD), Material.AIR, 0, ShopCategory.MELEE));
        addShopItem(21, new ShopItem("Stone Sword", new ItemStack(Material.STONE_SWORD), Material.IRON_INGOT, 10, ShopCategory.MELEE));
        addShopItem(22, new ShopItem("Iron Sword", new ItemStack(Material.IRON_SWORD), Material.GOLD_INGOT, 7, ShopCategory.MELEE));
        addShopItem(23, new ShopItem("Diamond Sword", new ItemStack(Material.DIAMOND_SWORD), Material.EMERALD, 4, ShopCategory.MELEE));
        addShopItem(24, new ShopItem("Knockback Stick", createEnchantedItem(Material.STICK, "Knockback Stick", org.bukkit.enchantments.Enchantment.KNOCKBACK, 1), Material.GOLD_INGOT, 10, ShopCategory.MELEE));

        // Armor (Category 2)
        addShopItem(20, new ShopItem("Chainmail Armor", new ItemStack(Material.CHAINMAIL_BOOTS), Material.IRON_INGOT, 40, ShopCategory.ARMOR));
        addShopItem(21, new ShopItem("Iron Armor", new ItemStack(Material.IRON_BOOTS), Material.GOLD_INGOT, 12, ShopCategory.ARMOR));
        addShopItem(22, new ShopItem("Diamond Armor", new ItemStack(Material.DIAMOND_BOOTS), Material.EMERALD, 6, ShopCategory.ARMOR));

        // Tools (Category 3)
        addShopItem(20, new ShopItem("Shears", new ItemStack(Material.SHEARS), Material.IRON_INGOT, 20, ShopCategory.TOOLS));
        addShopItem(21, new ShopItem("Pickaxe Upgrade", new ItemStack(Material.WOODEN_PICKAXE), Material.IRON_INGOT, 10, ShopCategory.TOOLS));
        addShopItem(22, new ShopItem("Axe Upgrade", new ItemStack(Material.WOODEN_AXE), Material.IRON_INGOT, 10, ShopCategory.TOOLS));

        // Ranged (Category 4)
        addShopItem(20, new ShopItem("Arrows", new ItemStack(Material.ARROW, 8), Material.GOLD_INGOT, 2, ShopCategory.RANGED));
        addShopItem(21, new ShopItem("Bow", new ItemStack(Material.BOW), Material.GOLD_INGOT, 12, ShopCategory.RANGED));
        addShopItem(22, new ShopItem("Bow (Power I)", createEnchantedItem(Material.BOW, "Bow (Power I)", org.bukkit.enchantments.Enchantment.POWER, 1), Material.GOLD_INGOT, 24, ShopCategory.RANGED));
        addShopItem(23, new ShopItem("Bow (Punch I, Power I)", createEnchantedItem(Material.BOW, "Bow (Punch I, Power I)", org.bukkit.enchantments.Enchantment.PUNCH, 1, org.bukkit.enchantments.Enchantment.POWER, 1), Material.EMERALD, 6, ShopCategory.RANGED));

        // Potions (Category 5)
        org.bukkit.potion.PotionType speedType = null;
        try {
            speedType = org.bukkit.potion.PotionType.valueOf("SPEED");
        } catch (IllegalArgumentException e) {
            try {
                speedType = org.bukkit.potion.PotionType.valueOf("SWIFTNESS");
            } catch (IllegalArgumentException e2) {
                // Fallback or handle error
            }
        }
        
        org.bukkit.potion.PotionType leapType = null;
        try {
            leapType = org.bukkit.potion.PotionType.valueOf("LEAPING");
        } catch (IllegalArgumentException e) {
            try {
                leapType = org.bukkit.potion.PotionType.valueOf("JUMP");
            } catch (IllegalArgumentException e2) {
            }
        }

        if (speedType != null) addShopItem(20, new ShopItem("Speed II Potion (45s)", createPotion(speedType, false, true), Material.EMERALD, 1, ShopCategory.POTIONS));
        if (leapType != null) addShopItem(21, new ShopItem("Jump V Potion (45s)", createPotion(leapType, false, true), Material.EMERALD, 1, ShopCategory.POTIONS));
        try {
            addShopItem(22, new ShopItem("Invisibility Potion (30s)", createPotion(org.bukkit.potion.PotionType.valueOf("INVISIBILITY"), false, false), Material.EMERALD, 1, ShopCategory.POTIONS));
        } catch (IllegalArgumentException e) {}

        // Utility (Category 6)
        addShopItem(20, new ShopItem("Golden Apple", new ItemStack(Material.GOLDEN_APPLE), Material.GOLD_INGOT, 3, ShopCategory.UTILITY));
        addShopItem(21, new ShopItem("Fireball", new ItemStack(Material.FIRE_CHARGE), Material.IRON_INGOT, 40, ShopCategory.UTILITY));
        addShopItem(22, new ShopItem("TNT", new ItemStack(Material.TNT), Material.GOLD_INGOT, 10, ShopCategory.UTILITY));
        addShopItem(23, new ShopItem("Ender Pearl", new ItemStack(Material.ENDER_PEARL), Material.EMERALD, 4, ShopCategory.UTILITY));
        addShopItem(24, new ShopItem("Water Bucket", new ItemStack(Material.WATER_BUCKET), Material.EMERALD, 1, ShopCategory.UTILITY));
        addShopItem(29, new ShopItem("Magic Milk", new ItemStack(Material.MILK_BUCKET), Material.GOLD_INGOT, 4, ShopCategory.UTILITY));
        addShopItem(30, new ShopItem("Bridge Egg", new ItemStack(Material.EGG), Material.EMERALD, 4, ShopCategory.UTILITY));
        addShopItem(31, new ShopItem("Iron Golem", new ItemStack(Material.IRON_GOLEM_SPAWN_EGG), Material.IRON_INGOT, 150, ShopCategory.UTILITY));
        ItemStack iceFish = new ItemStack(Material.SNOWBALL);
        ItemMeta iceFishMeta = iceFish.getItemMeta();
        iceFishMeta.setDisplayName(ChatColor.AQUA + "Ice Fish");
        iceFish.setItemMeta(iceFishMeta);
        addShopItem(32, new ShopItem("Ice Fish", iceFish, Material.IRON_INGOT, 50, ShopCategory.UTILITY));
    }

    private void addShopItem(int slot, ShopItem item) {
        shopItems.computeIfAbsent(item.getCategory(), k -> new HashMap<>()).put(slot, item);
    }

    private ItemStack createEnchantedItem(Material material, String name, Object... enchants) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.AQUA + name);
        for (int i = 0; i < enchants.length; i += 2) {
            meta.addEnchant((org.bukkit.enchantments.Enchantment) enchants[i], (int) enchants[i + 1], true);
        }
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createPotion(org.bukkit.potion.PotionType type, boolean extended, boolean upgraded) {
        ItemStack item = new ItemStack(Material.POTION);
        org.bukkit.inventory.meta.PotionMeta meta = (org.bukkit.inventory.meta.PotionMeta) item.getItemMeta();
        
        int duration = 0;
        int amplifier = upgraded ? 1 : 0;
        org.bukkit.potion.PotionEffectType effectType = null;
        
        if (type.name().equals("SPEED") || type.name().equals("SWIFTNESS")) {
            duration = 45 * 20;
            amplifier = 1; // Speed II
            effectType = org.bukkit.potion.PotionEffectType.SPEED;
        } else if (type.name().equals("LEAPING") || type.name().equals("JUMP")) {
            duration = 45 * 20;
            amplifier = 4; // Jump V
            effectType = org.bukkit.potion.PotionEffectType.JUMP_BOOST;
        } else if (type.name().equals("INVISIBILITY")) {
            duration = 30 * 20;
            amplifier = 0;
            effectType = org.bukkit.potion.PotionEffectType.INVISIBILITY;
        }
        
        if (effectType != null) {
            meta.addCustomEffect(new org.bukkit.potion.PotionEffect(effectType, duration, amplifier), true);
        }
        meta.setDisplayName(ChatColor.AQUA + type.name().replace("_", " ") + " Potion");
        item.setItemMeta(meta);
        return item;
    }

    public void openShop(Player player, ShopCategory category) {
        Inventory inv = Bukkit.createInventory(null, 54, "Shop - " + category.getDisplayName());

        // Fill background
        ItemStack glass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta glassMeta = glass.getItemMeta();
        glassMeta.setDisplayName(" ");
        glass.setItemMeta(glassMeta);
        
        // Fill entire inventory first
        for (int i = 0; i < 54; i++) inv.setItem(i, glass);

        // Categories
        for (ShopCategory cat : ShopCategory.values()) {
            ItemStack icon = new ItemStack(cat.getIcon());
            ItemMeta meta = icon.getItemMeta();
            meta.setDisplayName((cat == category ? ChatColor.YELLOW : ChatColor.GREEN) + cat.getDisplayName());
            icon.setItemMeta(meta);
            inv.setItem(cat.getSlot(), icon);
        }

        // Category Items
        Map<Integer, ShopItem> items = new HashMap<>();
        if (category == ShopCategory.QUICK_BUY) {
            Map<Integer, String> playerQB = SpyBedWars.getInstance().getQuickBuyManager().getQuickBuy(player);
            for (Map.Entry<Integer, String> qbEntry : playerQB.entrySet()) {
                ShopItem item = findItemByName(qbEntry.getValue());
                if (item != null) {
                    items.put(qbEntry.getKey(), item);
                }
            }

            // Show empty slots in Quick Buy
            int[] qbSlots = {19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34, 37, 38, 39, 40, 41, 42, 43};
            for (int slot : qbSlots) {
                if (!items.containsKey(slot)) {
                    ItemStack empty = new ItemStack(Material.RED_STAINED_GLASS_PANE);
                    ItemMeta meta = empty.getItemMeta();
                    meta.setDisplayName(ChatColor.RED + "Empty Slot");
                    List<String> lore = new ArrayList<>();
                    lore.add(ChatColor.GRAY + "Shift-Click an item in another");
                    lore.add(ChatColor.GRAY + "category to add it here!");
                    meta.setLore(lore);
                    empty.setItemMeta(meta);
                    inv.setItem(slot, empty);
                }
            }
        } else {
            Map<Integer, ShopItem> catItems = shopItems.get(category);
            if (catItems != null) items.putAll(catItems);
        }

        if (!items.isEmpty()) {
            for (Map.Entry<Integer, ShopItem> entry : items.entrySet()) {
                int slot = entry.getKey();
                
                // Don't overwrite category row
                if (slot < 9) continue;
                
                ShopItem item = entry.getValue();
                
                ItemStack display = item.getItemStack().clone();
                Material currency = item.getCurrency();
                int price = item.getPrice();

                // Handle Tool Upgrades display
                if (item.getName().contains("Pickaxe Upgrade") || item.getName().contains("Axe Upgrade")) {
                    com.spygamingog.spybedwars.arena.ArenaManager am = SpyBedWars.getInstance().getArenaManager();
                    java.util.Optional<com.spygamingog.spybedwars.arena.Arena> arenaOpt = am.getArenaByPlayer(player);
                    if (arenaOpt.isPresent()) {
                        com.spygamingog.spybedwars.arena.Arena arena = arenaOpt.get();
                        boolean isPickaxe = item.getName().contains("Pickaxe");
                        int level = isPickaxe ? 
                            arena.getPickaxeLevels().getOrDefault(player.getUniqueId(), 0) : 
                            arena.getAxeLevels().getOrDefault(player.getUniqueId(), 0);

                        if (level >= 4) {
                            display.setType(Material.BARRIER);
                            ItemMeta meta = display.getItemMeta();
                            meta.setDisplayName(ChatColor.RED + "Maxed Out!");
                            display.setItemMeta(meta);
                        } else {
                            // Update display item and price for next level
                            if (isPickaxe) {
                                switch (level) {
                                    case 0 -> { display.setType(Material.WOODEN_PICKAXE); currency = Material.IRON_INGOT; price = 10; }
                                    case 1 -> { display.setType(Material.STONE_PICKAXE); currency = Material.IRON_INGOT; price = 10; }
                                    case 2 -> { display.setType(Material.IRON_PICKAXE); currency = Material.GOLD_INGOT; price = 3; }
                                    case 3 -> { display.setType(Material.DIAMOND_PICKAXE); currency = Material.GOLD_INGOT; price = 6; }
                                }
                            } else {
                                switch (level) {
                                    case 0 -> { display.setType(Material.WOODEN_AXE); currency = Material.IRON_INGOT; price = 10; }
                                    case 1 -> { display.setType(Material.STONE_AXE); currency = Material.IRON_INGOT; price = 10; }
                                    case 2 -> { display.setType(Material.IRON_AXE); currency = Material.GOLD_INGOT; price = 3; }
                                    case 3 -> { display.setType(Material.DIAMOND_AXE); currency = Material.GOLD_INGOT; price = 6; }
                                }
                            }
                        }
                    }
                }

                // Handle TNT and Bridge Egg price display
                if (item.getName().equalsIgnoreCase("TNT") || item.getName().equalsIgnoreCase("Bridge Egg")) {
                    com.spygamingog.spybedwars.arena.ArenaManager am = SpyBedWars.getInstance().getArenaManager();
                    java.util.Optional<com.spygamingog.spybedwars.arena.Arena> arenaOpt = am.getArenaByPlayer(player);
                    if (arenaOpt.isPresent()) {
                        com.spygamingog.spybedwars.arena.Arena arena = arenaOpt.get();
                        boolean isSolo = arena.getArenaType().name().contains("SOLO") || arena.getArenaType().name().contains("DOUBLE");
                        if (item.getName().equalsIgnoreCase("TNT")) {
                            price = isSolo ? 5 : 10;
                        } else {
                            price = isSolo ? 2 : 4;
                        }
                    }
                }
                
                // Set wool color based on team
                if (display.getType().name().contains("WOOL") || display.getType() == Material.TERRACOTTA || display.getType() == Material.GLASS) {
                    com.spygamingog.spybedwars.arena.ArenaManager am = SpyBedWars.getInstance().getArenaManager();
                    java.util.Optional<com.spygamingog.spybedwars.arena.Arena> arena = am.getArenaByPlayer(player);
                    if (arena.isPresent()) {
                        com.spygamingog.spybedwars.arena.team.Team team = arena.get().getTeam(player);
                        if (team != null) {
                            if (display.getType().name().contains("WOOL")) {
                                display.setType(team.getColor().getWool());
                            } else if (display.getType() == Material.TERRACOTTA) {
                                display.setType(team.getColor().getTerracotta());
                            } else if (display.getType() == Material.GLASS) {
                                display.setType(team.getColor().getGlass());
                            }
                        }
                    }
                }

                ItemMeta meta = display.getItemMeta();
                List<String> lore = new ArrayList<>();
                if (currency == Material.AIR) {
                    lore.add(ChatColor.GRAY + "Cost: " + ChatColor.GREEN + "FREE");
                } else {
                    lore.add(ChatColor.GRAY + "Cost: " + getCurrencyColor(currency) + price + " " + currency.name().replace("_INGOT", ""));
                }
                lore.add("");
                lore.add(ChatColor.YELLOW + "Click to purchase!");
                meta.setLore(lore);
                display.setItemMeta(meta);
                
                inv.setItem(slot, display);
            }
        }

        player.openInventory(inv);
    }

    private ChatColor getCurrencyColor(Material material) {
        return switch (material) {
            case IRON_INGOT -> ChatColor.WHITE;
            case GOLD_INGOT -> ChatColor.GOLD;
            case EMERALD -> ChatColor.GREEN;
            default -> ChatColor.GRAY;
        };
    }

    public ShopItem getItemAtSlot(Player player, ShopCategory category, int slot) {
        if (category == ShopCategory.QUICK_BUY) {
            Map<Integer, String> playerQB = SpyBedWars.getInstance().getQuickBuyManager().getQuickBuy(player);
            String itemName = playerQB.get(slot);
            if (itemName != null) {
                return findItemByName(itemName);
            }
            return null;
        }
        Map<Integer, ShopItem> items = shopItems.get(category);
        if (items == null) return null;
        return items.get(slot);
    }

    public ShopItem findItemByName(String name) {
        for (Map<Integer, ShopItem> items : shopItems.values()) {
            for (ShopItem item : items.values()) {
                if (item.getName().equalsIgnoreCase(name)) {
                    return item;
                }
            }
        }
        return null;
    }
}
