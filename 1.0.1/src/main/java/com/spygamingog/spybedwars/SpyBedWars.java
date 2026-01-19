package com.spygamingog.spybedwars;

import com.spygamingog.spybedwars.arena.ArenaManager;
import com.spygamingog.spybedwars.commands.MainCommand;
import com.spygamingog.spybedwars.listeners.ArenaListener;
import com.spygamingog.spybedwars.listeners.GlobalListener;
import com.spygamingog.spybedwars.listeners.CombatListener;
import com.spygamingog.spybedwars.scoreboard.ScoreboardManager;
import com.spygamingog.spybedwars.shop.QuickBuyManager;
import com.spygamingog.spybedwars.shop.ShopListener;
import com.spygamingog.spybedwars.shop.ShopManager;
import com.spygamingog.spybedwars.shop.UpgradeListener;
import com.spygamingog.spybedwars.shop.UpgradeManager;
import com.spygamingog.spybedwars.managers.LobbyItemManager;
import lombok.Getter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.logging.Logger;

public class SpyBedWars extends JavaPlugin {

    @Getter private static SpyBedWars instance;
    private Logger logger;
    @Getter private ArenaManager arenaManager;
    @Getter private ScoreboardManager scoreboardManager;
    @Getter private UpgradeManager upgradeManager;
    @Getter private ShopManager shopManager;
    @Getter private QuickBuyManager quickBuyManager;
    @Getter private LobbyItemManager lobbyItemManager;
    @Getter private com.spygamingog.spybedwars.menu.MenuManager menuManager;
    @Getter private FileConfiguration messagesConfig;
    @Getter private com.spygamingog.spybedwars.npc.NPCManager npcManager;

    @Getter private com.spygamingog.spybedwars.listeners.CombatListener combatListener;

    @Override
    public void onEnable() {
        instance = this;
        this.logger = getLogger();

        saveDefaultConfig();
        createMessagesConfig();

        this.arenaManager = new ArenaManager();
        // Delay arena loading by 1 tick to ensure worlds are registered in SpyCore
        getServer().getScheduler().runTask(this, () -> arenaManager.loadArenas());
        
        this.scoreboardManager = new ScoreboardManager();
        this.shopManager = new ShopManager();
        this.upgradeManager = new UpgradeManager();
        this.quickBuyManager = new QuickBuyManager(this);
        this.lobbyItemManager = new LobbyItemManager();
        this.menuManager = new com.spygamingog.spybedwars.menu.MenuManager(this);
        this.npcManager = new com.spygamingog.spybedwars.npc.NPCManager();
        this.combatListener = new com.spygamingog.spybedwars.listeners.CombatListener();

        MainCommand mainCommand = new MainCommand();
        getCommand("bw").setExecutor(mainCommand);
        getCommand("bw").setTabCompleter(mainCommand);
        getServer().getPluginManager().registerEvents(new ArenaListener(), this);
        getServer().getPluginManager().registerEvents(combatListener, this);
        getServer().getPluginManager().registerEvents(new ShopListener(), this);
        
        getServer().getPluginManager().registerEvents(new GlobalListener(), this);
        getServer().getPluginManager().registerEvents(new UpgradeListener(), this);
        getServer().getPluginManager().registerEvents(new com.spygamingog.spybedwars.menu.MenuListener(this), this);
        getServer().getPluginManager().registerEvents(new com.spygamingog.spybedwars.listeners.LobbyListener(), this);

        logger.info("SpyBedWars 1.0.1 has been enabled!");
        logger.info("Developed by SpyGamingOG");

        // Reset combat for all online players on startup
        for (org.bukkit.entity.Player player : getServer().getOnlinePlayers()) {
            combatListener.resetCombat(player);
        }
    }

    @Override
    public void onDisable() {
        if (arenaManager != null) {
            arenaManager.getArenas().forEach(com.spygamingog.spybedwars.arena.Arena::stop);
        }
        if (scoreboardManager != null) {
            scoreboardManager.clearAll();
        }
        
        // Cancel all tasks
        getServer().getScheduler().cancelTasks(this);
        
        logger.info("SpyBedWars has been disabled and all tasks cancelled.");
    }

    private void createMessagesConfig() {
        File messagesFile = new File(getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            saveResource("messages.yml", false);
        }
        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
    }
}
