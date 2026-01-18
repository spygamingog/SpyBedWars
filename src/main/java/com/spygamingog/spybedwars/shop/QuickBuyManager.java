package com.spygamingog.spybedwars.shop;

import com.spygamingog.spybedwars.SpyBedWars;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class QuickBuyManager {

    private final SpyBedWars plugin;
    private final Map<UUID, Map<Integer, String>> playerQuickBuy = new HashMap<>();

    public QuickBuyManager(SpyBedWars plugin) {
        this.plugin = plugin;
    }

    public void loadPlayer(Player player) {
        File file = new File(plugin.getDataFolder() + "/quickbuy", player.getUniqueId() + ".yml");
        if (!file.exists()) {
            playerQuickBuy.put(player.getUniqueId(), getDefaultQuickBuy());
            return;
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        Map<Integer, String> items = new HashMap<>();
        if (config.contains("items")) {
            for (String key : config.getConfigurationSection("items").getKeys(false)) {
                items.put(Integer.parseInt(key), config.getString("items." + key));
            }
        }
        playerQuickBuy.put(player.getUniqueId(), items);
    }

    public void savePlayer(Player player) {
        Map<Integer, String> items = playerQuickBuy.get(player.getUniqueId());
        if (items == null) return;

        File file = new File(plugin.getDataFolder() + "/quickbuy", player.getUniqueId() + ".yml");
        if (!file.getParentFile().exists()) file.getParentFile().mkdirs();

        FileConfiguration config = new YamlConfiguration();
        for (Map.Entry<Integer, String> entry : items.entrySet()) {
            config.set("items." + entry.getKey(), entry.getValue());
        }

        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save quick buy for " + player.getName());
        }
    }

    public void unloadPlayer(Player player) {
        playerQuickBuy.remove(player.getUniqueId());
    }

    public Map<Integer, String> getQuickBuy(Player player) {
        return playerQuickBuy.computeIfAbsent(player.getUniqueId(), k -> {
            loadPlayer(player);
            return playerQuickBuy.get(player.getUniqueId());
        });
    }

    public void setQuickBuyItem(Player player, int slot, String itemName) {
        getQuickBuy(player).put(slot, itemName);
        savePlayer(player);
    }

    public void removeQuickBuyItem(Player player, int slot) {
        getQuickBuy(player).remove(slot);
        savePlayer(player);
    }

    private Map<Integer, String> getDefaultQuickBuy() {
        Map<Integer, String> defaults = new HashMap<>();
        defaults.put(19, "Wool");
        defaults.put(20, "Stone Sword");
        defaults.put(21, "Chainmail Armor");
        defaults.put(22, "Bow");
        defaults.put(23, "Speed II Potion (45s)");
        defaults.put(24, "TNT");
        defaults.put(25, "Golden Apple");
        return defaults;
    }
}
