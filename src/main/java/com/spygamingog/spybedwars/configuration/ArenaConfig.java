package com.spygamingog.spybedwars.configuration;

import com.spygamingog.spybedwars.SpyBedWars;
import com.spygamingog.spybedwars.arena.Arena;
import com.spygamingog.spybedwars.api.arena.ArenaType;
import com.spygamingog.spybedwars.arena.generator.Generator;
import com.spygamingog.spybedwars.arena.generator.GeneratorType;
import com.spygamingog.spybedwars.arena.team.Team;
import com.spygamingog.spybedwars.arena.team.TeamColor;
import com.spygamingog.spybedwars.utils.LocationUtil;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ArenaConfig {

    private final String arenaName;
    private final File file;
    private FileConfiguration config;

    public ArenaConfig(String arenaName) {
        this.arenaName = arenaName;
        File arenasFolder = new File(SpyBedWars.getInstance().getDataFolder(), "arenas");
        if (!arenasFolder.exists()) arenasFolder.mkdirs();
        this.file = new File(arenasFolder, arenaName + ".yml");
        this.config = YamlConfiguration.loadConfiguration(file);
    }

    public void saveArena(Arena arena) {
        config.set("name", arena.getArenaName());
        config.set("type", arena.getArenaType().name());
        config.set("waitingLobby", LocationUtil.serialize(arena.getWaitingLobby()));
        config.set("spectatorLocation", LocationUtil.serialize(arena.getSpectatorLocation()));
        config.set("minPlayers", arena.getMinPlayers());
        config.set("maxPlayers", arena.getMaxPlayers());
        config.set("enabled", arena.isEnabled());

        // Save Teams
        config.set("teams", null);
        for (Team team : arena.getTeams()) {
            String path = "teams." + team.getName();
            config.set(path + ".color", team.getColor().name());
            config.set(path + ".spawn", LocationUtil.serialize(team.getSpawnPoint()));
            config.set(path + ".bed", LocationUtil.serialize(team.getBedLocation()));
            config.set(path + ".shop", LocationUtil.serialize(team.getShopNPC()));
            config.set(path + ".upgrade", LocationUtil.serialize(team.getUpgradeNPC()));
        }

        // Save Generators
        List<String> generatorStrings = new ArrayList<>();
        for (Generator gen : arena.getGenerators()) {
            generatorStrings.add(gen.getType().name() + ";" + LocationUtil.serialize(gen.getLocation()));
        }
        config.set("generators", generatorStrings);

        try {
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Arena loadArena() {
        Arena arena = new Arena(arenaName);
        arena.setArenaType(ArenaType.valueOf(config.getString("type", "SOLO")));
        arena.setWaitingLobby(validate(LocationUtil.deserialize(config.getString("waitingLobby"))));
        arena.setSpectatorLocation(validate(LocationUtil.deserialize(config.getString("spectatorLocation"))));
        arena.setMinPlayers(config.getInt("minPlayers", SpyBedWars.getInstance().getConfig().getInt("defaults.min-players", 2)));
        arena.setMaxPlayers(config.getInt("maxPlayers", SpyBedWars.getInstance().getConfig().getInt("defaults.max-players", 8)));
        arena.setEnabled(config.getBoolean("enabled", false));

        // Load Teams
        ConfigurationSection teamsSection = config.getConfigurationSection("teams");
        if (teamsSection != null) {
            for (String teamName : teamsSection.getKeys(false)) {
                String path = "teams." + teamName;
                TeamColor color = TeamColor.valueOf(config.getString(path + ".color", "WHITE"));
                Team team = new Team(teamName, color);
                team.setSpawnPoint(validate(LocationUtil.deserialize(config.getString(path + ".spawn"))));
                team.setBedLocation(validate(LocationUtil.deserialize(config.getString(path + ".bed"))));
                team.setShopNPC(validate(LocationUtil.deserialize(config.getString(path + ".shop"))));
                team.setUpgradeNPC(validate(LocationUtil.deserialize(config.getString(path + ".upgrade"))));
                arena.getTeams().add(team);
            }
        }

        // Load Generators
        List<String> generatorStrings = config.getStringList("generators");
        for (String genStr : generatorStrings) {
            String[] parts = genStr.split(";");
            if (parts.length == 2) {
                GeneratorType type = GeneratorType.valueOf(parts[0]);
                arena.getGenerators().add(new Generator(validate(LocationUtil.deserialize(parts[1])), type));
            }
        }

        return arena;
    }

    private org.bukkit.Location validate(org.bukkit.Location loc) {
        return LocationUtil.validate(loc);
    }
}
