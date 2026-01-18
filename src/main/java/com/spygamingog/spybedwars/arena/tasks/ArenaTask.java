package com.spygamingog.spybedwars.arena.tasks;

import com.spygamingog.spybedwars.SpyBedWars;
import com.spygamingog.spybedwars.api.arena.GameState;
import com.spygamingog.spybedwars.arena.Arena;
import com.spygamingog.spybedwars.arena.team.Team;
import lombok.Getter;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class ArenaTask extends BukkitRunnable {

    private final Arena arena;
    @Getter
    private int countdown;

    public ArenaTask(Arena arena) {
        this.arena = arena;
        this.countdown = SpyBedWars.getInstance().getConfig().getInt("defaults.countdown-waiting", 30);
    }

    private int playingTime = 0;
    private int nextPhaseTime = 600; // 10 minutes for first phase
    private int phase = 0;

    @Override
    public void run() {
        if (!arena.isEnabled()) {
            return;
        }
        
        // Update Scoreboard for all players
        arena.getPlayers().forEach(uuid -> {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) {
                SpyBedWars.getInstance().getScoreboardManager().updateScoreboard(p, arena);
            }
        });

        if (arena.getGameState() == GameState.WAITING) {
            if (arena.getPlayers().size() >= arena.getMinPlayers()) {
                if (arena.getTeams().isEmpty()) {
                    // Cannot start without teams
                    return;
                }
                arena.setGameState(GameState.STARTING);
                this.countdown = SpyBedWars.getInstance().getConfig().getInt("defaults.countdown-starting", 10);
                broadcastMessage("game.starting", "{time}", String.valueOf(countdown));
            }
        } else if (arena.getGameState() == GameState.STARTING) {
            if (arena.getPlayers().size() < arena.getMinPlayers()) {
                arena.setGameState(GameState.WAITING);
                this.countdown = SpyBedWars.getInstance().getConfig().getInt("defaults.countdown-waiting", 30);
                broadcastMessage("game.not-enough-players");
                return;
            }

            if (countdown > 0) {
                if (countdown <= 5 || countdown % 10 == 0) {
                    broadcastMessage("game.starting", "{time}", String.valueOf(countdown));
                }
                countdown--;
            } else {
                assignTeams();
                arena.setGameState(GameState.PLAYING);
                arena.updatePlayerNames();

                // Clear potential pre-game drops
                arena.getGenerators().forEach(gen -> {
                    gen.getLocation().getWorld().getNearbyEntities(gen.getLocation(), 2, 2, 2).stream()
                        .filter(e -> e instanceof org.bukkit.entity.Item)
                        .forEach(org.bukkit.entity.Entity::remove);
                });

                broadcastMessage("game.started");
                // Start game mechanics here
            }
        } else if (arena.getGameState() == GameState.PLAYING) {
            arena.getGenerators().forEach(generator -> generator.tick());
            checkWinner();
            handlePhases();
            handleHealPool();
        }
    }

    private void handleHealPool() {
        for (Team team : arena.getTeams()) {
            if (team.getUpgrades().getOrDefault("heal_pool", 0) > 0) {
                for (UUID uuid : team.getMembers()) {
                    Player p = Bukkit.getPlayer(uuid);
                    if (p != null && p.getGameMode() != org.bukkit.GameMode.SPECTATOR) {
                        // Check if player is near their spawn (island)
                        if (team.getSpawnPoint() != null && p.getLocation().getWorld().equals(team.getSpawnPoint().getWorld()) && 
                            p.getLocation().distanceSquared(team.getSpawnPoint()) < 900) { // 30 block radius
                            p.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.REGENERATION, 50, 0, false, false, false));
                        }
                    }
                }
            }
        }
    }

    private void handlePhases() {
        playingTime++;
        if (playingTime >= nextPhaseTime) {
            phase++;
            triggerNextPhase();
        }
    }

    private void triggerNextPhase() {
        switch (phase) {
            case 1 -> { // Diamond II
                broadcastMessage("game.phase.diamond-2");
                arena.getGenerators().stream()
                    .filter(g -> g.getType() == com.spygamingog.spybedwars.arena.generator.GeneratorType.DIAMOND)
                    .forEach(g -> g.setDelay(g.getDelay() / 2));
                nextPhaseTime = playingTime + 600;
            }
            case 2 -> { // Emerald II
                broadcastMessage("game.phase.emerald-2");
                arena.getGenerators().stream()
                    .filter(g -> g.getType() == com.spygamingog.spybedwars.arena.generator.GeneratorType.EMERALD)
                    .forEach(g -> g.setDelay(g.getDelay() / 2));
                nextPhaseTime = playingTime + 600;
            }
            case 3 -> { // Bed Destruction
                broadcastMessage("game.phase.bed-destruction");
                for (com.spygamingog.spybedwars.arena.team.Team team : arena.getTeams()) {
                    team.setBedBroken(true);
                    if (team.getBedLocation() != null) {
                        team.getBedLocation().getBlock().setType(org.bukkit.Material.AIR);
                    }
                }
                nextPhaseTime = playingTime + 600;
            }
            case 4 -> { // Sudden Death
                broadcastMessage("game.phase.sudden-death");
                // Spawn dragons logic would go here
                nextPhaseTime = playingTime + 600;
            }
            case 5 -> { // Game End
                broadcastMessage("game.phase.ending");
                arena.setGameState(com.spygamingog.spybedwars.api.arena.GameState.ENDING);
            }
        }
    }

    private void checkWinner() {
        if (arena.getGameState() != GameState.PLAYING) return;

        java.util.List<Team> activeTeams = arena.getTeams().stream()
                .filter(team -> team.getMembers().stream().anyMatch(uuid -> {
                    Player p = Bukkit.getPlayer(uuid);
                    if (p == null || !arena.getPlayers().contains(uuid)) return false;
                    
                    // A player is considered active if:
                    // 1. They are not in spectator mode
                    // 2. OR they are in spectator mode but their team's bed is NOT broken (they are respawning)
                    return p.getGameMode() != org.bukkit.GameMode.SPECTATOR || !team.isBedBroken();
                }))
                .collect(java.util.stream.Collectors.toList());

        if (activeTeams.size() <= 1) {
            if (activeTeams.size() == 1) {
                Team winner = activeTeams.get(0);
                broadcastMessage("game.winner", "{team_color}", winner.getColor().getChatColor().toString(), "{team_name}", winner.getName());
            } else {
                broadcastMessage("game.no-winner");
            }
            stopGame();
        }
    }

    private void stopGame() {
        arena.setGameState(GameState.ENDING);
        // NPCs and Holograms are now persistent
        
        Bukkit.getScheduler().runTaskLater(SpyBedWars.getInstance(), () -> {
            // Remove all blocks placed during game
            arena.clearPlacedBlocks();
            
            // Reset teams and players
            arena.getTeams().forEach(team -> {
                team.getMembers().clear();
                team.setBedBroken(false);
            });
            
            // Teleport players back to lobby
            String lobbyStr = SpyBedWars.getInstance().getConfig().getString("main-lobby");
            org.bukkit.Location rawLobby = lobbyStr != null ? com.spygamingog.spybedwars.utils.LocationUtil.deserialize(lobbyStr) : null;
            final org.bukkit.Location lobby = rawLobby != null ? com.spygamingog.spybedwars.utils.LocationUtil.validate(rawLobby) : null;
            
            arena.getPlayers().forEach(uuid -> {
                Player p = Bukkit.getPlayer(uuid);
                if (p != null) {
                    p.setGameMode(org.bukkit.GameMode.ADVENTURE);
                    p.setAllowFlight(true);
                    p.setFlying(true);
                    p.getInventory().clear();
                    p.getInventory().setArmorContents(null);
                    p.getActivePotionEffects().forEach(effect -> p.removePotionEffect(effect.getType()));
                    p.setHealth(20);
                    p.setFoodLevel(20);
                    p.setExp(0);
                    p.setLevel(0);
                    
                    // Visibility fix
                    for (Player online : Bukkit.getOnlinePlayers()) {
                        online.showPlayer(SpyBedWars.getInstance(), p);
                        p.showPlayer(SpyBedWars.getInstance(), online);
                    }
                    
                    // Refresh NPCs for player
                    SpyBedWars.getInstance().getNpcManager().onPlayerJoin(p);
                    
                    if (lobby != null) {
                        p.teleport(lobby);
                        SpyBedWars.getInstance().getLobbyItemManager().giveLobbyItems(p);
                    }
                    p.setDisplayName(p.getName());
                    p.setPlayerListName(p.getName());
                }
            });
            
            arena.getPlayers().clear();
            arena.getKills().clear();
            arena.getFinalKills().clear();
            arena.setGameState(GameState.WAITING);
            this.countdown = SpyBedWars.getInstance().getConfig().getInt("defaults.countdown-waiting", 30);
            this.playingTime = 0;
            this.phase = 0;
            this.nextPhaseTime = 600;
        }, 20 * 10); // 10 seconds delay before reset
    }

    private void assignTeams() {
        if (arena.getTeams().isEmpty()) return;

        List<UUID> unassignedPlayers = new ArrayList<>();
        for (UUID uuid : arena.getPlayers()) {
            boolean assigned = arena.getTeams().stream().anyMatch(t -> t.getMembers().contains(uuid));
            if (!assigned) {
                unassignedPlayers.add(uuid);
            }
        }

        for (UUID uuid : unassignedPlayers) {
            Player p = Bukkit.getPlayer(uuid);
            if (p == null) continue;

            Team smallestTeam = arena.getTeams().stream()
                    .min(java.util.Comparator.comparingInt(t -> t.getMembers().size()))
                    .orElse(arena.getTeams().get(0));

            smallestTeam.addMember(p);
        }

        // Teleport and equip all players
        for (Team team : arena.getTeams()) {
            for (UUID uuid : team.getMembers()) {
                Player p = Bukkit.getPlayer(uuid);
                if (p == null) continue;

                if (team.getSpawnPoint() != null) {
                    p.teleport(team.getSpawnPoint());
                    p.setGameMode(org.bukkit.GameMode.SURVIVAL);
                    p.setAllowFlight(false);
                    p.setFlying(false);
                    p.setFoodLevel(20);
                    p.setHealth(20);
                    p.getInventory().clear();
                    p.getInventory().addItem(new org.bukkit.inventory.ItemStack(org.bukkit.Material.WOODEN_SWORD));
                    equipTeamArmor(p, team);
                }
            }
        }
    }

    private void equipTeamArmor(Player p, Team team) {
        org.bukkit.Color color = team.getColor().getColor();

        ItemStack helmet = new ItemStack(org.bukkit.Material.LEATHER_HELMET);
        ItemStack chestplate = new ItemStack(org.bukkit.Material.LEATHER_CHESTPLATE);
        ItemStack leggings = new ItemStack(org.bukkit.Material.LEATHER_LEGGINGS);
        ItemStack boots = new ItemStack(org.bukkit.Material.LEATHER_BOOTS);

        org.bukkit.inventory.meta.LeatherArmorMeta meta;

        meta = (org.bukkit.inventory.meta.LeatherArmorMeta) helmet.getItemMeta();
        meta.setColor(color);
        helmet.setItemMeta(meta);

        meta = (org.bukkit.inventory.meta.LeatherArmorMeta) chestplate.getItemMeta();
        meta.setColor(color);
        chestplate.setItemMeta(meta);

        meta = (org.bukkit.inventory.meta.LeatherArmorMeta) leggings.getItemMeta();
        meta.setColor(color);
        leggings.setItemMeta(meta);

        meta = (org.bukkit.inventory.meta.LeatherArmorMeta) boots.getItemMeta();
        meta.setColor(color);
        boots.setItemMeta(meta);

        p.getInventory().setHelmet(helmet);
        p.getInventory().setChestplate(chestplate);
        p.getInventory().setLeggings(leggings);
        p.getInventory().setBoots(boots);
    }

    private void broadcastMessage(String path, String... placeholders) {
        String prefix = SpyBedWars.getInstance().getMessagesConfig().getString("prefix", "&8[&bSpyBedWars&8] ");
        String msg = SpyBedWars.getInstance().getMessagesConfig().getString(path, path);
        for (int i = 0; i < placeholders.length; i += 2) {
            msg = msg.replace(placeholders[i], placeholders[i + 1]);
        }
        String finalMsg = ChatColor.translateAlternateColorCodes('&', prefix + msg);
        TextComponent component = new TextComponent(finalMsg);
        component.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(ChatColor.GRAY + "Game Notification")));
        
        if (path.contains("winner")) {
            component.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/bw leave"));
        } else if (path.contains("starting")) {
            component.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/bw list"));
        }

        arena.getPlayers().forEach(uuid -> {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) p.spigot().sendMessage(component);
        });
    }
}
