package com.spygamingog.spybedwars.arena;

import com.spygamingog.spybedwars.SpyBedWars;
import com.spygamingog.spybedwars.api.arena.ArenaType;
import com.spygamingog.spybedwars.api.arena.GameState;
import com.spygamingog.spybedwars.arena.generator.Generator;
import com.spygamingog.spybedwars.arena.tasks.ArenaTask;
import com.spygamingog.spybedwars.arena.team.Team;
import lombok.Getter;
import lombok.Setter;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class Arena {

    private final String arenaName;
    private GameState gameState;
    private ArenaType arenaType;
    private final List<UUID> players;
    private final List<Team> teams;
    private final List<Generator> generators;
    private final java.util.Set<String> placedBlockKeys = new java.util.concurrent.ConcurrentHashMap<String, Boolean>().keySet(true);
    private final java.util.Map<java.util.UUID, Integer> pickaxeLevels = new java.util.concurrent.ConcurrentHashMap<>();
    private final java.util.Map<java.util.UUID, Integer> axeLevels = new java.util.concurrent.ConcurrentHashMap<>();
    private final java.util.Map<java.util.UUID, Integer> armorLevels = new java.util.concurrent.ConcurrentHashMap<>();
    private final java.util.Map<java.util.UUID, Boolean> hasShears = new java.util.concurrent.ConcurrentHashMap<>();
    private final java.util.Map<UUID, Integer> kills;
    private final java.util.Map<UUID, Integer> finalKills;
    private final java.util.Map<UUID, Integer> bedBreaks = new java.util.concurrent.ConcurrentHashMap<>();
    
    private Location waitingLobby;
    private Location spectatorLocation;
    
    private int minPlayers;
    private int maxPlayers;
    private boolean enabled;

    private ArenaTask arenaTask;
    
    public Arena(String arenaName) {
        this.arenaName = arenaName;
        this.gameState = GameState.WAITING;
        this.arenaType = ArenaType.SOLO;
        this.players = new java.util.concurrent.CopyOnWriteArrayList<>();
        this.teams = new ArrayList<>();
        this.generators = new ArrayList<>();
        this.kills = new java.util.concurrent.ConcurrentHashMap<>();
        this.finalKills = new java.util.concurrent.ConcurrentHashMap<>();
        // Task will be started when arena is enabled
    }

    public void addPlacedBlock(Location loc) {
        if (loc == null || !isWorldLoaded(loc)) return;
        placedBlockKeys.add(getBlockKey(loc));
    }

    public void removePlacedBlock(Location loc) {
        if (loc == null || !isWorldLoaded(loc)) return;
        placedBlockKeys.remove(getBlockKey(loc));
    }

    public boolean isPlacedBlock(Location loc) {
        if (loc == null || !isWorldLoaded(loc)) return false;
        return placedBlockKeys.contains(getBlockKey(loc));
    }

    public void clearPlacedBlocks() {
        for (String key : placedBlockKeys) {
            Location loc = parseBlockKey(key);
            if (loc != null && isWorldLoaded(loc)) {
                loc.getBlock().setType(org.bukkit.Material.AIR);
            }
        }
        placedBlockKeys.clear();
    }

    private boolean isWorldLoaded(Location loc) {
        try {
            return loc.getWorld() != null;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private Location parseBlockKey(String key) {
        String[] parts = key.split(",");
        if (parts.length != 4) return null;
        org.bukkit.World world = org.bukkit.Bukkit.getWorld(parts[0]);
        if (world == null) return null;
        try {
            int x = Integer.parseInt(parts[1]);
            int y = Integer.parseInt(parts[2]);
            int z = Integer.parseInt(parts[3]);
            return new Location(world, x, y, z);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String getBlockKey(Location loc) {
        String worldName = null;
        try {
            if (loc.getWorld() != null) worldName = loc.getWorld().getName();
        } catch (IllegalArgumentException e) {
            String locStr = loc.toString();
            if (locStr.contains("world=")) {
                int start = locStr.indexOf("world=") + 6;
                int end = locStr.indexOf(",", start);
                if (end != -1) worldName = locStr.substring(start, end);
            }
        }
        return (worldName != null ? worldName : "null") + "," + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
    }

    public void startTask() {
        if (arenaTask == null) {
            this.arenaTask = new ArenaTask(this);
            this.arenaTask.runTaskTimer(SpyBedWars.getInstance(), 0, 20);
            
            // Spawn persistent elements when task starts for enabled arenas
            spawnPersistentElements();
        }
    }

    public void spawnPersistentElements() {
        // Ensure all locations are validated and worlds are loaded
        validateLocations();

        // Spawn Generator Holograms
        generators.forEach(com.spygamingog.spybedwars.arena.generator.Generator::spawnHologram);

        // Spawn Team NPCs
        teams.forEach(team -> {
            if (team.getShopNPC() != null) {
                SpyBedWars.getInstance().getNpcManager().spawnShopNPC(
                            team.getShopNPC(),
                            arenaName,
                            team.getName()
                );
            }
            if (team.getUpgradeNPC() != null) {
                SpyBedWars.getInstance().getNpcManager().spawnUpgradeNPC(
                            team.getUpgradeNPC(),
                            arenaName,
                            team.getName()
                );
            }
            if (team.getBedLocation() != null) {
                spawnBed(team.getBedLocation(), team.getColor().getBed());
                spawnBedHologram(team);
            }
        });
    }

    private void spawnBedHologram(com.spygamingog.spybedwars.arena.team.Team team) {
        if (team.getBedLocation() == null) return;
        
        // If hologram already exists and is valid, don't respawn
        if (team.getBedHologram() != null && team.getBedHologram().isValid()) {
            return;
        }

        // Remove old hologram if it exists but is invalid
        if (team.getBedHologram() != null) {
            team.getBedHologram().remove();
            team.setBedHologram(null);
        }

        org.bukkit.Bukkit.getScheduler().runTaskLater(SpyBedWars.getInstance(), () -> {
            if (team.getBedLocation() == null || team.getBedLocation().getWorld() == null) return;

            // Cleanup any orphaned bed holograms nearby
            team.getBedLocation().getWorld().getEntitiesByClass(org.bukkit.entity.ArmorStand.class).stream()
                .filter(as -> as.getLocation().distanceSquared(team.getBedLocation()) < 4)
                .filter(as -> as.hasMetadata("spybedwars_bed") || (as.getCustomName() != null && as.getCustomName().contains("Bed")))
                .forEach(org.bukkit.entity.Entity::remove);

            org.bukkit.Location loc = team.getBedLocation().clone().add(0, 1.8, 0);
            org.bukkit.entity.ArmorStand hologram = loc.getWorld().spawn(loc, org.bukkit.entity.ArmorStand.class, as -> {
                as.setBasePlate(false);
                as.setVisible(false);
                as.setGravity(false);
                as.setCanPickupItems(false);
                as.setCustomNameVisible(true);
                as.setSmall(true);
                as.setMarker(true);
                as.setMetadata("spybedwars_bed", new org.bukkit.metadata.FixedMetadataValue(com.spygamingog.spybedwars.SpyBedWars.getInstance(), true));
                as.setCustomName(team.getColor().getChatColor() + "" + org.bukkit.ChatColor.BOLD + team.getName() + " Bed");
            });
            
            team.setBedHologram(hologram);
            if (hologram != null && hologram.isValid()) {
                SpyBedWars.getInstance().getLogger().info("Spawned bed hologram for team " + team.getName());
            }
        }, 1L);
    }

    private void spawnBed(org.bukkit.Location location, org.bukkit.Material bedMaterial) {
        float yaw = (location.getYaw() % 360 + 360) % 360;
        org.bukkit.block.BlockFace facing;
        if (yaw >= 45 && yaw < 135) facing = org.bukkit.block.BlockFace.WEST;
        else if (yaw >= 135 && yaw < 225) facing = org.bukkit.block.BlockFace.NORTH;
        else if (yaw >= 225 && yaw < 315) facing = org.bukkit.block.BlockFace.EAST;
        else facing = org.bukkit.block.BlockFace.SOUTH;

        org.bukkit.block.Block foot = location.getBlock();
        org.bukkit.block.Block head = foot.getRelative(facing);

        // Clear existing blocks first to avoid issues
        head.setType(org.bukkit.Material.AIR, false);
        foot.setType(org.bukkit.Material.AIR, false);

        head.setType(bedMaterial, false);
        foot.setType(bedMaterial, false);

        if (head.getBlockData() instanceof org.bukkit.block.data.type.Bed headData) {
            headData.setPart(org.bukkit.block.data.type.Bed.Part.HEAD);
            headData.setFacing(facing);
            head.setBlockData(headData, false);
        }

        if (foot.getBlockData() instanceof org.bukkit.block.data.type.Bed footData) {
            footData.setPart(org.bukkit.block.data.type.Bed.Part.FOOT);
            footData.setFacing(facing);
            foot.setBlockData(footData, false);
        }
    }

    public void stop() {
        if (arenaTask != null) {
            arenaTask.cancel();
            arenaTask = null;
        }
        
        // Actually remove the blocks from the world
        clearPlacedBlocks();
        
        // Reset all game-related data
        kills.clear();
        finalKills.clear();
        bedBreaks.clear();
        pickaxeLevels.clear();
        axeLevels.clear();
        armorLevels.clear();
        hasShears.clear();
        
        // Reset team states
        teams.forEach(team -> {
            team.setBedBroken(false);
            team.getMembers().clear();
        });
        
        // NPCs and Holograms are now persistent and won't be removed on stop
    }

    public void cleanupPersistentElements() {
        // Remove Generator Holograms
        generators.forEach(com.spygamingog.spybedwars.arena.generator.Generator::remove);

        // Remove Team NPCs and Bed Holograms
        teams.forEach(team -> {
            if (team.getBedHologram() != null) {
                team.getBedHologram().remove();
                team.setBedHologram(null);
            }
        });

        // Use NPCManager to remove NPCs for this arena
        SpyBedWars.getInstance().getNpcManager().removeNPCs(this);
    }

    public void validateLocations() {
        this.waitingLobby = com.spygamingog.spybedwars.utils.LocationUtil.validate(waitingLobby);
        this.spectatorLocation = com.spygamingog.spybedwars.utils.LocationUtil.validate(spectatorLocation);
        
        generators.forEach(gen -> {
            gen.setLocation(com.spygamingog.spybedwars.utils.LocationUtil.validate(gen.getLocation()));
        });

        teams.forEach(team -> {
            team.setShopNPC(com.spygamingog.spybedwars.utils.LocationUtil.validate(team.getShopNPC()));
            team.setUpgradeNPC(com.spygamingog.spybedwars.utils.LocationUtil.validate(team.getUpgradeNPC()));
            team.setSpawnPoint(com.spygamingog.spybedwars.utils.LocationUtil.validate(team.getSpawnPoint()));
            team.setBedLocation(com.spygamingog.spybedwars.utils.LocationUtil.validate(team.getBedLocation()));
        });
    }

    public void addPlayer(Player player) {
        if (players.size() >= maxPlayers) {
            TextComponent msg = new TextComponent(org.bukkit.ChatColor.RED + "This arena is full!");
            msg.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(org.bukkit.ChatColor.GRAY + "Click to join another arena")));
            msg.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/bw list"));
            player.spigot().sendMessage(msg);
            return;
        }

        // Validate all locations to ensure world is loaded and objects are up-to-date
        validateLocations();

        // Clear any old data for this player to prevent carry-over from previous games
        UUID uuid = player.getUniqueId();
        kills.remove(uuid);
        finalKills.remove(uuid);
        bedBreaks.remove(uuid);
        pickaxeLevels.remove(uuid);
        axeLevels.remove(uuid);
        armorLevels.remove(uuid);
        hasShears.remove(uuid);

        players.add(uuid);
        player.teleport(waitingLobby);
        player.setGameMode(org.bukkit.GameMode.ADVENTURE);
        player.setAllowFlight(false);
        player.setFlying(false);
        
        SpyBedWars.getInstance().getLobbyItemManager().giveWaitingItems(player);
        SpyBedWars.getInstance().getCombatListener().resetCombat(player);

        broadcastMessage(org.bukkit.ChatColor.YELLOW + player.getName() + " has joined (" + players.size() + "/" + maxPlayers + ")!");

        if (players.size() >= minPlayers && gameState == GameState.WAITING) {
            setGameState(GameState.STARTING);
        }
    }

    public void removePlayer(Player player) {
        players.remove(player.getUniqueId());
        teams.forEach(team -> team.getMembers().remove(player.getUniqueId()));
        
        // Cleanup game levels
        pickaxeLevels.remove(player.getUniqueId());
        axeLevels.remove(player.getUniqueId());
        armorLevels.remove(player.getUniqueId());
        hasShears.remove(player.getUniqueId());
        bedBreaks.remove(player.getUniqueId());
        kills.remove(player.getUniqueId());
        finalKills.remove(player.getUniqueId());
        
        // Reset player state
        player.setGameMode(org.bukkit.GameMode.ADVENTURE);
        player.setAllowFlight(false);
        player.setFlying(false);
        player.getInventory().clear();
        player.setHealth(20);
        player.setFoodLevel(20);
        player.setDisplayName(player.getName());
        player.setPlayerListName(player.getName());
        SpyBedWars.getInstance().getScoreboardManager().removeScoreboard(player);
        SpyBedWars.getInstance().getCombatListener().resetCombat(player);
        
        // Visibility fix
        for (Player online : org.bukkit.Bukkit.getOnlinePlayers()) {
            online.showPlayer(SpyBedWars.getInstance(), player);
            player.showPlayer(SpyBedWars.getInstance(), online);
        }
        
        // Refresh NPCs for player
        SpyBedWars.getInstance().getNpcManager().onPlayerJoin(player);
        
        String lobbyStr = SpyBedWars.getInstance().getConfig().getString("main-lobby");
        if (lobbyStr != null) {
            player.teleport(com.spygamingog.spybedwars.utils.LocationUtil.deserialize(lobbyStr));
            SpyBedWars.getInstance().getLobbyItemManager().giveLobbyItems(player);
        } else {
            // Fallback: teleport to spawn of the default world if no lobby set
            player.teleport(org.bukkit.Bukkit.getWorlds().get(0).getSpawnLocation());
        }

        broadcastMessage(org.bukkit.ChatColor.YELLOW + player.getName() + " has left (" + players.size() + "/" + maxPlayers + ")!");

        if (players.size() < minPlayers && gameState == GameState.STARTING) {
            setGameState(GameState.WAITING);
            broadcastMessage(org.bukkit.ChatColor.RED + "Not enough players! Countdown cancelled.");
        }

        if (players.isEmpty() && gameState != GameState.WAITING) {
            stop();
            setGameState(GameState.WAITING);
        }
    }

    public void updatePlayerNames() {
        for (java.util.UUID uuid : players) {
            Player player = org.bukkit.Bukkit.getPlayer(uuid);
            if (player == null) continue;

            Team team = teams.stream()
                    .filter(t -> t.getMembers().contains(uuid))
                    .findFirst().orElse(null);

            if (team != null) {
                String name = team.getColor().getChatColor() + player.getName();
                player.setDisplayName(name);
                player.setPlayerListName(name);
            } else {
                String name = org.bukkit.ChatColor.GRAY + "[SPEC] " + player.getName();
                player.setDisplayName(name);
                player.setPlayerListName(name);
            }
        }
    }

    public void setGameState(GameState gameState) {
        this.gameState = gameState;
        
        if (gameState == GameState.PLAYING) {
            // Validate all locations one last time before starting
            validateLocations();

            // Teleport players to their team spawns and spawn beds for active teams
            teams.forEach(team -> {
                if (!team.getMembers().isEmpty()) {
                    // Spawn bed only for teams with players
                    if (team.getBedLocation() != null) {
                        spawnBed(team.getBedLocation(), team.getColor().getBed());
                        spawnBedHologram(team);
                    }

                    // Teleport members
                    team.getMembers().forEach(uuid -> {
                        Player p = org.bukkit.Bukkit.getPlayer(uuid);
                        if (p != null) {
                            p.teleport(team.getSpawnPoint());
                            p.getInventory().clear();
                            p.setHealth(20);
                            p.setFoodLevel(20);
                            p.setGameMode(org.bukkit.GameMode.SURVIVAL);
                        }
                    });
                }
            });

            // Apply 1.8 combat to all players when game starts
            players.forEach(uuid -> {
                Player p = org.bukkit.Bukkit.getPlayer(uuid);
                if (p != null) {
                    SpyBedWars.getInstance().getCombatListener().set18Combat(p);
                }
            });
        } else if (gameState == GameState.WAITING || gameState == GameState.STARTING) {
            // Reset combat for all players when game is not playing
            players.forEach(uuid -> {
                Player p = org.bukkit.Bukkit.getPlayer(uuid);
                if (p != null) {
                    SpyBedWars.getInstance().getCombatListener().resetCombat(p);
                }
            });
        }
    }

    public boolean canEnable() {
        if (waitingLobby == null) return false;
        if (spectatorLocation == null) return false;
        if (teams.isEmpty()) return false;
        if (generators.isEmpty()) return false;
        
        for (Team team : teams) {
            if (team.getSpawnPoint() == null) return false;
            if (team.getBedLocation() == null) return false;
            if (team.getShopNPC() == null) return false;
            if (team.getUpgradeNPC() == null) return false;
        }
        
        return true;
    }

    public void broadcastMessage(String message) {
        TextComponent component = new TextComponent(message);
        component.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(org.bukkit.ChatColor.GRAY + "Arena Notification")));
        component.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/bw help"));
        
        for (UUID uuid : players) {
            Player player = org.bukkit.Bukkit.getPlayer(uuid);
            if (player != null) {
                player.spigot().sendMessage(component);
            }
        }
    }

    public Team getTeam(Player player) {
        return teams.stream()
                .filter(team -> team.getMembers().contains(player.getUniqueId()))
                .findFirst().orElse(null);
    }
}
