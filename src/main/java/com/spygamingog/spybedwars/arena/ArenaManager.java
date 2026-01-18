package com.spygamingog.spybedwars.arena;

import com.spygamingog.spybedwars.SpyBedWars;
import com.spygamingog.spybedwars.configuration.ArenaConfig;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ArenaManager {

    private final List<Arena> arenas;

    public ArenaManager() {
        this.arenas = new ArrayList<>();
    }

    private void cleanupOldHolograms() {
        org.bukkit.Bukkit.getWorlds().forEach(world -> {
            world.getEntitiesByClass(org.bukkit.entity.ArmorStand.class).forEach(as -> {
                if (as.hasMetadata("spybedwars_generator") || as.hasMetadata("spybedwars_bed") ||
                    (as.getCustomName() != null && (as.getCustomName().contains("Generator") || as.getCustomName().contains("Bed")))) {
                    as.remove();
                }
            });
        });
    }

    public void loadArenas() {
        cleanupOldHolograms();
        File arenasFolder = new File(SpyBedWars.getInstance().getDataFolder(), "arenas");
        if (!arenasFolder.exists()) arenasFolder.mkdirs();

        File[] files = arenasFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) {
            SpyBedWars.getInstance().getLogger().info("No arenas found to load.");
            return;
        }

        SpyBedWars.getInstance().getLogger().info("Loading " + files.length + " arenas...");
        for (File file : files) {
            try {
                String arenaName = file.getName().replace(".yml", "");
                ArenaConfig arenaConfig = new ArenaConfig(arenaName);
                Arena arena = arenaConfig.loadArena();
                
                if (arena.getTeams().isEmpty()) {
                    SpyBedWars.getInstance().getLogger().warning("Arena " + arenaName + " has no teams! Skipping.");
                    continue;
                }
                
                if (arena.getWaitingLobby() == null) {
                    SpyBedWars.getInstance().getLogger().warning("Arena " + arenaName + " has no waiting lobby! (World might not be loaded). Skipping.");
                    continue;
                }

                arenas.add(arena);
                if (arena.isEnabled()) {
                    arena.startTask();
                } else {
                    // Even if not enabled, spawn persistent elements for setup visibility
                    arena.spawnPersistentElements();
                }
                SpyBedWars.getInstance().getLogger().info("Loaded arena: " + arenaName + " (Enabled: " + arena.isEnabled() + ", Teams: " + arena.getTeams().size() + ")");
            } catch (Exception e) {
                SpyBedWars.getInstance().getLogger().severe("Failed to load arena from file: " + file.getName());
                e.printStackTrace();
            }
        }
        SpyBedWars.getInstance().getLogger().info("Successfully loaded " + arenas.size() + " arenas.");
    }

    public Optional<Arena> getArenaByLocation(org.bukkit.Location loc) {
        if (loc == null || loc.getWorld() == null) return Optional.empty();
        String worldName = loc.getWorld().getName();

        return arenas.stream()
            .filter(arena -> {
                // Check waiting lobby
                org.bukkit.Location lobby = arena.getWaitingLobby();
                if (lobby != null) {
                    // Use a helper to check if worlds match by name and if distance is close
                    if (isWithinRange(lobby, loc)) return true;
                }
                
                // Check team spawns
                return arena.getTeams().stream().anyMatch(t -> isWithinRange(t.getSpawnPoint(), loc));
            })
            .findFirst();
    }

    private boolean isWithinRange(org.bukkit.Location loc1, org.bukkit.Location loc2) {
        if (loc1 == null || loc2 == null) return false;
        
        // Get world names to compare (handles stale/unloaded world objects)
        String world1 = getWorldNameSafe(loc1);
        String world2 = getWorldNameSafe(loc2);
        
        if (world1 == null || world2 == null || !world1.equals(world2)) return false;
        
        try {
            double dx = loc1.getX() - loc2.getX();
            double dy = loc1.getY() - loc2.getY();
            double dz = loc1.getZ() - loc2.getZ();
            return (dx * dx + dy * dy + dz * dz) < 250000; // 500 blocks
        } catch (Exception e) {
            return false;
        }
    }

    private String getWorldNameSafe(org.bukkit.Location loc) {
        if (loc == null) return null;
        try {
            org.bukkit.World world = loc.getWorld();
            if (world != null) return world.getName();
        } catch (IllegalArgumentException e) {
            // World unloaded, try to extract name from toString
            String locStr = loc.toString();
            if (locStr.contains("world=")) {
                int start = locStr.indexOf("world=") + 6;
                int end = locStr.indexOf(",", start);
                if (end != -1) {
                    return locStr.substring(start, end);
                }
            }
        }
        return null;
    }

    public List<Arena> getArenas() {
        return new ArrayList<>(arenas);
    }

    public void addArena(Arena arena) {
        arenas.add(arena);
    }

    public boolean deleteArena(String name) {
        Optional<Arena> arenaOpt = getArenaByName(name);
        if (arenaOpt.isPresent()) {
            Arena arena = arenaOpt.get();
            arena.stop();
            arena.cleanupPersistentElements();
            arenas.remove(arena);
            File arenasFolder = new File(SpyBedWars.getInstance().getDataFolder(), "arenas");
            File file = new File(arenasFolder, name + ".yml");
            if (file.exists()) {
                return file.delete();
            }
        }
        return false;
    }

    public Arena cloneArena(Arena original, String newName) {
        Arena clone = new Arena(newName);
        clone.setArenaType(original.getArenaType());
        clone.setWaitingLobby(original.getWaitingLobby());
        clone.setSpectatorLocation(original.getSpectatorLocation());
        clone.setMinPlayers(original.getMinPlayers());
        clone.setMaxPlayers(original.getMaxPlayers());
        
        // Clone teams
        original.getTeams().forEach(team -> {
            com.spygamingog.spybedwars.arena.team.Team teamClone = new com.spygamingog.spybedwars.arena.team.Team(team.getName(), team.getColor());
            teamClone.setSpawnPoint(team.getSpawnPoint());
            teamClone.setBedLocation(team.getBedLocation());
            teamClone.setShopNPC(team.getShopNPC());
            teamClone.setUpgradeNPC(team.getUpgradeNPC());
            clone.getTeams().add(teamClone);
        });

        // Clone generators
        original.getGenerators().forEach(gen -> {
            clone.getGenerators().add(new com.spygamingog.spybedwars.arena.generator.Generator(gen.getLocation(), gen.getType()));
        });

        addArena(clone);
        new ArenaConfig(newName).saveArena(clone);
        return clone;
    }

    public Optional<Arena> getArenaByName(String name) {
        return arenas.stream()
                .filter(arena -> arena.getArenaName().equalsIgnoreCase(name))
                .findFirst();
    }

    public Optional<Arena> getArenaByPlayer(Player player) {
        return arenas.stream()
                .filter(arena -> arena.getPlayers().contains(player.getUniqueId()))
                .findFirst();
    }

    public org.bukkit.Location getLobbyLocation() {
        String lobbyStr = SpyBedWars.getInstance().getConfig().getString("main-lobby");
        return lobbyStr != null ? com.spygamingog.spybedwars.utils.LocationUtil.deserialize(lobbyStr) : null;
    }
}
