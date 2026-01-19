package com.spygamingog.spybedwars.commands;

import com.spygamingog.spybedwars.SpyBedWars;
import com.spygamingog.spybedwars.api.arena.ArenaType;
import com.spygamingog.spybedwars.arena.Arena;
import com.spygamingog.spybedwars.arena.ArenaManager;
import com.spygamingog.spybedwars.arena.generator.Generator;
import com.spygamingog.spybedwars.arena.generator.GeneratorType;
import com.spygamingog.spybedwars.arena.team.Team;
import com.spygamingog.spybedwars.arena.team.TeamColor;
import com.spygamingog.spybedwars.configuration.ArenaConfig;
import com.spygamingog.spybedwars.utils.LocationUtil;
import com.spygamingog.spybedwars.shop.ShopCategory;
import org.bukkit.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class MainCommand implements CommandExecutor, TabCompleter {

    private final ArenaManager arenaManager;

    public MainCommand() {
        this.arenaManager = SpyBedWars.getInstance().getArenaManager();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }

        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "create":
                if (args.length < 2) {
                    sendUsage(player, "/bw create <name>");
                    return true;
                }
                String name = args[1];
                if (arenaManager.getArenaByName(name).isPresent()) {
                    sendMessage(player, "setup.arena-exists");
                    return true;
                }
                Arena newArena = new Arena(name);
                arenaManager.addArena(newArena);
                new ArenaConfig(name).saveArena(newArena);
                TextComponent createdMsg = new TextComponent(getMessage("setup.arena-created").replace("{name}", name));
                createdMsg.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(ChatColor.GRAY + "Click to setup " + ChatColor.AQUA + name)));
                createdMsg.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/bw setupArena " + name));
                player.spigot().sendMessage(createdMsg);
                break;

            case "addteam": {
                if (args.length < 3) {
                    sendUsage(player, "/bw addteam <arena> <color>");
                    return true;
                }
                String arenaName = args[1];
                String colorName = args[2];

                TeamColor color;
                try {
                    color = TeamColor.valueOf(colorName.toUpperCase());
                } catch (IllegalArgumentException e) {
                    TextComponent msg = new TextComponent(ChatColor.RED + "Invalid team color! Use one of: " + 
                        Arrays.stream(TeamColor.values()).map(Enum::name).collect(Collectors.joining(", ")));
                    msg.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(ChatColor.GRAY + "Click for help")));
                    msg.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/bw help"));
                    player.spigot().sendMessage(msg);
                    return true;
                }

                arenaManager.getArenaByName(arenaName).ifPresentOrElse(arena -> {
                    if (arena.getTeams().stream().anyMatch(t -> t.getColor() == color)) {
                        TextComponent msg = new TextComponent(ChatColor.RED + "Team with color " + color.name() + " already exists in this arena!");
                        msg.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(ChatColor.GRAY + "Try another color.")));
                    msg.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/bw help"));
                    player.spigot().sendMessage(msg);
                        return;
                    }
                    Team newTeam = new Team(color.name(), color);
                    arena.getTeams().add(newTeam);
                    new ArenaConfig(arena.getArenaName()).saveArena(arena);
                    
                    TextComponent success = new TextComponent(ChatColor.GREEN + "Team " + color.name() + " added to arena " + arenaName);
                    success.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(ChatColor.GRAY + "Click to setup team spawns")));
                    success.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/bw setspawn " + arenaName + " " + color.name()));
                    player.spigot().sendMessage(success);
                    sendSetupStatus(player, arena);
                }, () -> {
                    TextComponent error = new TextComponent(ChatColor.RED + "Arena not found!");
                    error.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(ChatColor.GRAY + "Click to list arenas")));
                    error.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/bw list"));
                    player.spigot().sendMessage(error);
                });
                break;
            }

            case "setwaiting": {
                // Flexible: /bw setwaiting [arena]
                String setWaitingArenaName = null;
                if (args.length >= 2) {
                    setWaitingArenaName = args[1];
                }

                Optional<Arena> setWaitingArena;
                if (setWaitingArenaName != null) {
                    setWaitingArena = arenaManager.getArenaByName(setWaitingArenaName);
                } else {
                    setWaitingArena = arenaManager.getArenaByLocation(player.getLocation());
                }

                setWaitingArena.ifPresentOrElse(arena -> {
                    arena.setWaitingLobby(player.getLocation());
                    
                    new ArenaConfig(arena.getArenaName()).saveArena(arena);
                    TextComponent success = new TextComponent(getMessage("setup.waiting-set").replace("{arena}", arena.getArenaName()));
                    success.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(ChatColor.GRAY + "Click to check setup status")));
                    success.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/bw setupArena " + arena.getArenaName()));
                    player.spigot().sendMessage(success);
                    sendSetupStatus(player, arena);
                }, () -> {
                    TextComponent error = new TextComponent(ChatColor.RED + "Arena not found! Stand near the arena or specify its name.");
                    error.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(ChatColor.GRAY + "Click to list arenas")));
                    error.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/bw list"));
                    player.spigot().sendMessage(error);
                });
                break;
            }

            case "setspawn": {
                // Flexible: /bw setspawn [arena] <team>
                String setSpawnArenaName = null;
                String setSpawnTeamName = null;

                if (args.length == 2) {
                    setSpawnTeamName = args[1];
                } else if (args.length >= 3) {
                    setSpawnArenaName = args[1];
                    setSpawnTeamName = args[2];
                } else {
                    sendUsage(player, "/bw setspawn [arena] <team>");
                    return true;
                }

                TeamColor spawnColor;
                try {
                    spawnColor = TeamColor.valueOf(setSpawnTeamName.toUpperCase());
                } catch (IllegalArgumentException e) {
                    TextComponent msg = new TextComponent(ChatColor.RED + "Invalid team color! Use one of: " + 
                        Arrays.stream(TeamColor.values()).map(Enum::name).collect(Collectors.joining(", ")));
                    msg.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(ChatColor.GRAY + "Click for help")));
                    msg.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/bw help"));
                    player.spigot().sendMessage(msg);
                    return true;
                }

                Optional<Arena> setSpawnArena;
                if (setSpawnArenaName != null) {
                    setSpawnArena = arenaManager.getArenaByName(setSpawnArenaName);
                } else {
                    setSpawnArena = arenaManager.getArenaByLocation(player.getLocation());
                }

                setSpawnArena.ifPresentOrElse(arena -> {
                    String finalTeamName = spawnColor.name();
                    Team team = arena.getTeams().stream()
                            .filter(t -> t.getName().equalsIgnoreCase(finalTeamName))
                            .findFirst()
                            .orElseGet(() -> {
                                Team newTeam = new Team(finalTeamName, spawnColor);
                                arena.getTeams().add(newTeam);
                                return newTeam;
                            });

                    team.setSpawnPoint(player.getLocation());
                    new ArenaConfig(arena.getArenaName()).saveArena(arena);
                    TextComponent success = new TextComponent(getMessage("setup.spawn-set").replace("{team}", finalTeamName).replace("{arena}", arena.getArenaName()));
                    success.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(ChatColor.GRAY + "Click to set bed location")));
                    success.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/bw setbed " + arena.getArenaName() + " " + finalTeamName));
                    player.spigot().sendMessage(success);
                    sendSetupStatus(player, arena);
                }, () -> {
                    TextComponent error = new TextComponent(ChatColor.RED + "Arena not found! Stand near the arena or specify its name.");
                    error.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(ChatColor.GRAY + "Click to list arenas")));
                    error.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/bw list"));
                    player.spigot().sendMessage(error);
                });
                break;
            }

            case "setbed": {
                // Flexible: /bw setbed [arena] <team>
                String setBedArenaName = null;
                String setBedTeamName = null;

                if (args.length == 2) {
                    setBedTeamName = args[1];
                } else if (args.length >= 3) {
                    setBedArenaName = args[1];
                    setBedTeamName = args[2];
                } else {
                    sendUsage(player, "/bw setbed [arena] <team>");
                    return true;
                }

                TeamColor bedColor;
                try {
                    bedColor = TeamColor.valueOf(setBedTeamName.toUpperCase());
                } catch (IllegalArgumentException e) {
                    TextComponent msg = new TextComponent(ChatColor.RED + "Invalid team color! Use one of: " + 
                        Arrays.stream(TeamColor.values()).map(Enum::name).collect(Collectors.joining(", ")));
                    msg.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(ChatColor.GRAY + "Click for help")));
                    msg.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/bw help"));
                    player.spigot().sendMessage(msg);
                    return true;
                }

                Optional<Arena> setBedArena;
                if (setBedArenaName != null) {
                    setBedArena = arenaManager.getArenaByName(setBedArenaName);
                } else {
                    setBedArena = arenaManager.getArenaByLocation(player.getLocation());
                }

                setBedArena.ifPresentOrElse(arena -> {
                    String finalTeamName = bedColor.name();
                    Team team = arena.getTeams().stream()
                            .filter(t -> t.getName().equalsIgnoreCase(finalTeamName))
                            .findFirst()
                            .orElseGet(() -> {
                                Team newTeam = new Team(finalTeamName, bedColor);
                                arena.getTeams().add(newTeam);
                                return newTeam;
                            });

                    Location loc = player.getLocation();
                    float yaw = loc.getYaw();
                    // Snap yaw to nearest 90 degrees for better bed placement
                    if (yaw < 0) yaw += 360;
                    if (yaw >= 315 || yaw < 45) yaw = 0; // South
                    else if (yaw >= 45 && yaw < 135) yaw = 90; // West
                    else if (yaw >= 135 && yaw < 225) yaw = 180; // North
                    else yaw = 270; // East
                    loc.setYaw(yaw);
                    loc.setPitch(0); // Bed doesn't care about pitch

                    team.setBedLocation(loc);
                arena.spawnPersistentElements(); // Refresh visibility
                new ArenaConfig(arena.getArenaName()).saveArena(arena);
                TextComponent success = new TextComponent(getMessage("setup.bed-set").replace("{team}", finalTeamName).replace("{arena}", arena.getArenaName()));
                success.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(ChatColor.GRAY + "Click to set shop NPC")));
                success.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/bw setshop " + arena.getArenaName() + " " + finalTeamName));
                player.spigot().sendMessage(success);
                sendSetupStatus(player, arena);
            }, () -> {
                TextComponent error = new TextComponent(ChatColor.RED + "Arena not found! Stand near the arena or specify its name.");
                error.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(ChatColor.GRAY + "Click to list arenas")));
                error.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/bw list"));
                player.spigot().sendMessage(error);
            });
            break;
        }

        case "removespawn": {
            // /bw removespawn [arena] <team>
            String removeSpawnArenaName = null;
            String removeSpawnTeamName = null;

            if (args.length == 2) {
                removeSpawnTeamName = args[1];
            } else if (args.length >= 3) {
                removeSpawnArenaName = args[1];
                removeSpawnTeamName = args[2];
            } else {
                sendUsage(player, "/bw removespawn [arena] <team>");
                return true;
            }

            String finalTeamName = removeSpawnTeamName.toUpperCase();
            Optional<Arena> removeSpawnArena;
            if (removeSpawnArenaName != null) {
                removeSpawnArena = arenaManager.getArenaByName(removeSpawnArenaName);
            } else {
                removeSpawnArena = arenaManager.getArenaByLocation(player.getLocation());
            }

            removeSpawnArena.ifPresentOrElse(arena -> {
                Optional<Team> teamOpt = arena.getTeams().stream()
                        .filter(t -> t.getName().equalsIgnoreCase(finalTeamName))
                        .findFirst();

                if (teamOpt.isPresent()) {
                    Team team = teamOpt.get();
                    team.setSpawnPoint(null);
                    new ArenaConfig(arena.getArenaName()).saveArena(arena);
                    player.sendMessage(ChatColor.GREEN + "Spawn point for team " + finalTeamName + " removed from arena " + arena.getArenaName());
                    sendSetupStatus(player, arena);
                } else {
                    player.sendMessage(ChatColor.RED + "Team " + finalTeamName + " not found in arena " + arena.getArenaName());
                }
            }, () -> {
                player.sendMessage(ChatColor.RED + "Arena not found!");
            });
            break;
        }

        case "removebed": {
            // /bw removebed [arena] <team>
            String removeBedArenaName = null;
            String removeBedTeamName = null;

            if (args.length == 2) {
                removeBedTeamName = args[1];
            } else if (args.length >= 3) {
                removeBedArenaName = args[1];
                removeBedTeamName = args[2];
            } else {
                sendUsage(player, "/bw removebed [arena] <team>");
                return true;
            }

            String finalTeamName = removeBedTeamName.toUpperCase();
            Optional<Arena> removeBedArena;
            if (removeBedArenaName != null) {
                removeBedArena = arenaManager.getArenaByName(removeBedArenaName);
            } else {
                removeBedArena = arenaManager.getArenaByLocation(player.getLocation());
            }

            removeBedArena.ifPresentOrElse(arena -> {
                Optional<Team> teamOpt = arena.getTeams().stream()
                        .filter(t -> t.getName().equalsIgnoreCase(finalTeamName))
                        .findFirst();

                if (teamOpt.isPresent()) {
                    Team team = teamOpt.get();
                    if (team.getBedHologram() != null) {
                        team.getBedHologram().remove();
                        team.setBedHologram(null);
                    }
                    team.setBedLocation(null);
                    new ArenaConfig(arena.getArenaName()).saveArena(arena);
                    player.sendMessage(ChatColor.GREEN + "Bed for team " + finalTeamName + " removed from arena " + arena.getArenaName());
                    sendSetupStatus(player, arena);
                } else {
                    player.sendMessage(ChatColor.RED + "Team " + finalTeamName + " not found in arena " + arena.getArenaName());
                }
            }, () -> {
                player.sendMessage(ChatColor.RED + "Arena not found!");
            });
            break;
        }

        case "setup": {
            // /bw setup <arena> generators
            if (args.length < 3 || !args[2].equalsIgnoreCase("generators")) {
                sendUsage(player, "/bw setup <arena> generators");
                return true;
            }

            arenaManager.getArenaByName(args[1]).ifPresentOrElse(arena -> {
                Location center = player.getLocation();
                int radius = 100;
                int diamondCount = 0;
                int emeraldCount = 0;

                player.sendMessage(ChatColor.YELLOW + "Scanning for Diamond and Emerald blocks within 100 blocks...");

                for (int x = -radius; x <= radius; x++) {
                    for (int y = -radius; y <= radius; y++) {
                        for (int z = -radius; z <= radius; z++) {
                            org.bukkit.block.Block block = center.clone().add(x, y, z).getBlock();
                            GeneratorType type = null;
                            if (block.getType() == org.bukkit.Material.DIAMOND_BLOCK) {
                                if (isIsolated(block, org.bukkit.Material.DIAMOND_BLOCK)) {
                                    type = GeneratorType.DIAMOND;
                                    diamondCount++;
                                }
                            } else if (block.getType() == org.bukkit.Material.EMERALD_BLOCK) {
                                if (isIsolated(block, org.bukkit.Material.EMERALD_BLOCK)) {
                                    type = GeneratorType.EMERALD;
                                    emeraldCount++;
                                }
                            }

                            if (type != null) {
                                Location genLoc = block.getLocation().add(0.5, 1, 0.5);
                                // Check if generator already exists at this location
                                boolean exists = arena.getGenerators().stream()
                                        .anyMatch(g -> g.getLocation().getWorld().equals(genLoc.getWorld()) && 
                                                      g.getLocation().distanceSquared(genLoc) < 0.1);
                                
                                if (!exists) {
                                    Generator newGen = new Generator(genLoc, type);
                                    arena.getGenerators().add(newGen);
                                    newGen.spawnHologram();
                                }
                            }
                        }
                    }
                }

                new ArenaConfig(arena.getArenaName()).saveArena(arena);
                player.sendMessage(ChatColor.GREEN + "Generator setup complete for arena " + arena.getArenaName());
                player.sendMessage(ChatColor.AQUA + "Added " + diamondCount + " Diamond and " + emeraldCount + " Emerald generators.");
                if (diamondCount == 0 && emeraldCount == 0) {
                    player.sendMessage(ChatColor.YELLOW + "No isolated Diamond or Emerald blocks were found within 100 blocks.");
                }
                player.sendMessage(ChatColor.GRAY + "(Note: Only single, isolated blocks were added to avoid structural blocks)");
                sendSetupStatus(player, arena);
            }, () -> {
                player.sendMessage(ChatColor.RED + "Arena not found!");
            });
            break;
        }

            case "addgenerator":
                // /bw addgenerator <arena> <type>
                if (args.length < 3) {
                    sendUsage(player, "/bw addgenerator <arena> <IRON|GOLD|DIAMOND|EMERALD>");
                    return true;
                }
                arenaManager.getArenaByName(args[1]).ifPresentOrElse(arena -> {
                    try {
                        GeneratorType type = GeneratorType.valueOf(args[2].toUpperCase());
                        Generator newGen = new Generator(player.getLocation(), type);
                        arena.getGenerators().add(newGen);
                        newGen.spawnHologram(); // Spawn hologram immediately for setup visibility
                        new ArenaConfig(arena.getArenaName()).saveArena(arena);
                        
                        TextComponent success = new TextComponent(getMessage("setup.generator-added").replace("{type}", type.name()).replace("{arena}", arena.getArenaName()));
                        success.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(ChatColor.GRAY + "Click to check setup status")));
                        success.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/bw setupArena " + arena.getArenaName()));
                        player.spigot().sendMessage(success);
                        
                        player.sendMessage(ChatColor.GRAY + "(Hologram spawned at your location. If not visible, try moving slightly and re-adding)");
                        sendSetupStatus(player, arena);
                    } catch (IllegalArgumentException e) {
                        TextComponent msg = new TextComponent(ChatColor.RED + "Invalid generator type! Use: IRON, GOLD, DIAMOND, EMERALD");
                        msg.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(ChatColor.GRAY + "Click for help")));
                        msg.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/bw help"));
                        player.spigot().sendMessage(msg);
                    }
                }, () -> {
                    TextComponent error = new TextComponent(ChatColor.RED + "Arena not found!");
                    error.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(ChatColor.GRAY + "Click to list arenas")));
                    error.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/bw list"));
                    player.spigot().sendMessage(error);
                });
                break;

            case "refreshholograms":
                if (args.length < 2) {
                    sendUsage(player, "/bw refreshholograms <arena>");
                    return true;
                }
                arenaManager.getArenaByName(args[1]).ifPresentOrElse(arena -> {
                    arena.spawnPersistentElements();
                    player.sendMessage(ChatColor.GREEN + "Attempted to refresh all holograms for arena " + arena.getArenaName());
                }, () -> {
                    player.sendMessage(ChatColor.RED + "Arena not found!");
                });
                break;

            case "join":
                if (args.length < 2) {
                    sendUsage(player, "/bw join <arena>");
                    return true;
                }
                arenaManager.getArenaByName(args[1]).ifPresentOrElse(arena -> {
                    if (!arena.isEnabled()) {
                        TextComponent msg = new TextComponent(ChatColor.RED + "This arena is currently disabled!");
                        msg.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(ChatColor.GRAY + "Try another arena.")));
                        msg.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/bw list"));
                        player.spigot().sendMessage(msg);
                        return;
                    }
                    if (arena.getWaitingLobby() == null) {
                        TextComponent msg = new TextComponent(ChatColor.RED + "Arena lobby not set!");
                        msg.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(ChatColor.GRAY + "Contact an admin.")));
                        msg.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/bw help"));
                        player.spigot().sendMessage(msg);
                        return;
                    }

                    // Validate world is loaded (handles hibernation)
                    Location lobby = LocationUtil.validate(arena.getWaitingLobby());
                    if (lobby == null || lobby.getWorld() == null) {
                        player.sendMessage(ChatColor.RED + "Failed to load arena world!");
                        return;
                    }

                    arena.addPlayer(player);
                    TextComponent success = new TextComponent(ChatColor.GREEN + "Joined " + arena.getArenaName());
                    success.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(ChatColor.GRAY + "Good luck!")));
                    success.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/bw leave"));
                    player.spigot().sendMessage(success);
                }, () -> {
                    TextComponent error = new TextComponent(ChatColor.RED + "Arena not found!");
                    error.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(ChatColor.GRAY + "Click to list arenas")));
                    error.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/bw list"));
                    player.spigot().sendMessage(error);
                });
                break;

            case "list":
                TextComponent header = new TextComponent(ChatColor.GOLD + "--- Arena List ---");
                header.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(ChatColor.GRAY + "Click an arena to join!")));
                player.spigot().sendMessage(header);
                for (Arena arena : arenaManager.getArenas()) {
                    String status = arena.isEnabled() ? ChatColor.GREEN + "ENABLED" : ChatColor.RED + "DISABLED";
                    TextComponent arenaMsg = new TextComponent(ChatColor.YELLOW + "- " + arena.getArenaName() + " [" + status + ChatColor.YELLOW + "] (" + arena.getPlayers().size() + "/" + arena.getMaxPlayers() + ")");
                    
                    arenaMsg.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(ChatColor.GRAY + "Click to join " + ChatColor.AQUA + arena.getArenaName())));
                    arenaMsg.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/bw join " + arena.getArenaName()));
                    
                    player.spigot().sendMessage(arenaMsg);
                }
                break;

            case "delete":
                if (args.length < 2) {
                    sendUsage(player, "/bw delete <arena>");
                    return true;
                }
                if (arenaManager.deleteArena(args[1])) {
                    TextComponent success = new TextComponent(ChatColor.GREEN + "Arena " + args[1] + " deleted successfully.");
                    success.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(ChatColor.GRAY + "Arena removed from system.")));
                    success.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/bw list"));
                    player.spigot().sendMessage(success);
                } else {
                    TextComponent error = new TextComponent(ChatColor.RED + "Arena not found!");
                    error.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(ChatColor.GRAY + "Click to list arenas")));
                    error.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/bw list"));
                    player.spigot().sendMessage(error);
                }
                break;

            case "enable":
                if (args.length < 2) {
                    sendUsage(player, "/bw enable <arena>");
                    return true;
                }
                arenaManager.getArenaByName(args[1]).ifPresentOrElse(arena -> {
                    if (arena.canEnable()) {
                        arena.setEnabled(true);
                        arena.startTask(); // Start the task so the arena is active
                        TextComponent enabledMsg = new TextComponent(ChatColor.GREEN + "Arena " + arena.getArenaName() + " has been enabled!");
                        enabledMsg.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(ChatColor.GRAY + "Click to join " + ChatColor.AQUA + arena.getArenaName())));
                        enabledMsg.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/bw join " + arena.getArenaName()));
                        player.spigot().sendMessage(enabledMsg);
                    } else {
                        TextComponent error = new TextComponent(ChatColor.RED + "Arena " + arena.getArenaName() + " is not fully set up yet!");
                        error.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(ChatColor.GRAY + "Click to check requirements")));
                        error.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/bw setupArena " + arena.getArenaName()));
                        player.spigot().sendMessage(error);
                    }
                }, () -> {
                    TextComponent error = new TextComponent(ChatColor.RED + "Arena not found!");
                    error.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(ChatColor.GRAY + "Click to list arenas")));
                    error.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/bw list"));
                    player.spigot().sendMessage(error);
                });
                break;

            case "disable":
                if (args.length < 2) {
                    sendUsage(player, "/bw disable <arena>");
                    return true;
                }
                arenaManager.getArenaByName(args[1]).ifPresentOrElse(arena -> {
                    arena.setEnabled(false);
                    TextComponent msg = new TextComponent(ChatColor.YELLOW + "Arena " + arena.getArenaName() + " has been disabled.");
                    msg.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(ChatColor.GRAY + "Arena is now offline.")));
                    msg.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/bw list"));
                    player.spigot().sendMessage(msg);
                }, () -> {
                    TextComponent error = new TextComponent(ChatColor.RED + "Arena not found!");
                    error.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(ChatColor.GRAY + "Click to list arenas")));
                    error.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/bw list"));
                    player.spigot().sendMessage(error);
                });
                break;

            case "clone":
                if (args.length < 3) {
                    sendUsage(player, "/bw clone <original> <new_name>");
                    return true;
                }
                arenaManager.getArenaByName(args[1]).ifPresentOrElse(original -> {
                    if (arenaManager.getArenaByName(args[2]).isPresent()) {
                        TextComponent msg = new TextComponent(ChatColor.RED + "An arena with the name " + args[2] + " already exists!");
                        msg.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(ChatColor.GRAY + "Try another name.")));
                        player.spigot().sendMessage(msg);
                        return;
                    }
                    arenaManager.cloneArena(original, args[2]);
                    TextComponent success = new TextComponent(ChatColor.GREEN + "Arena " + args[1] + " cloned to " + args[2] + " successfully!");
                    success.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(ChatColor.GRAY + "Click to setup " + ChatColor.AQUA + args[2])));
                    success.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/bw setupArena " + args[2]));
                    player.spigot().sendMessage(success);
                }, () -> {
                    TextComponent error = new TextComponent(ChatColor.RED + "Arena not found!");
                    error.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(ChatColor.GRAY + "Click to list arenas")));
                    error.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/bw list"));
                    player.spigot().sendMessage(error);
                });
                break;

            case "setuparena":
                if (args.length < 2) {
                    sendUsage(player, "/bw setupArena <arena>");
                    return true;
                }
                arenaManager.getArenaByName(args[1]).ifPresentOrElse(arena -> {
                    sendSetupStatus(player, arena);
                }, () -> {
                    TextComponent error = new TextComponent(ChatColor.RED + "Arena not found!");
                    error.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(ChatColor.GRAY + "Click to list arenas")));
                    error.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/bw list"));
                    player.spigot().sendMessage(error);
                });
                break;

            case "leave":
                arenaManager.getArenaByPlayer(player).ifPresentOrElse(arena -> {
                    arena.removePlayer(player);
                    TextComponent msg = new TextComponent(ChatColor.GREEN + "You have left the game.");
                    msg.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(ChatColor.GRAY + "Returning to lobby.")));
                    player.spigot().sendMessage(msg);
                }, () -> {
                    TextComponent msg = new TextComponent(ChatColor.RED + "You are not in a game!");
                    msg.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(ChatColor.GRAY + "Click to join an arena")));
                    msg.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/bw list"));
                    player.spigot().sendMessage(msg);
                });
                break;

            case "remove":
                if (args.length < 2) {
                    sendUsage(player, "/bw remove <generator|shop|upgrade>");
                    return true;
                }
                String typeArg = args[1].toLowerCase();
                if (typeArg.equals("generator")) {
                    // Shift arguments to mimic /bw removegenerator
                    String[] newArgs = new String[args.length - 1];
                    newArgs[0] = "removegenerator";
                    System.arraycopy(args, 2, newArgs, 1, args.length - 2);
                    return onCommand(sender, command, label, newArgs);
                } else if (typeArg.equals("shop")) {
                    // Mimic /bw removeshop
                    String[] newArgs = new String[args.length - 1];
                    newArgs[0] = "removeshop";
                    System.arraycopy(args, 2, newArgs, 1, args.length - 2);
                    return onCommand(sender, command, label, newArgs);
                } else if (typeArg.equals("upgrade")) {
                    // Mimic /bw removeupgrade
                    String[] newArgs = new String[args.length - 1];
                    newArgs[0] = "removeupgrade";
                    System.arraycopy(args, 2, newArgs, 1, args.length - 2);
                    return onCommand(sender, command, label, newArgs);
                }
                break;

            case "removegenerator": {
                // Flexible: /bw removegenerator [arena] <type>
                String arenaName = null;
                String genTypeName = null;

                if (args.length == 2) {
                    genTypeName = args[1];
                } else if (args.length >= 3) {
                    arenaName = args[1];
                    genTypeName = args[2];
                } else {
                    sendUsage(player, "/bw removegenerator [arena] <type>");
                    return true;
                }

                GeneratorType type;
                try {
                    type = GeneratorType.valueOf(genTypeName.toUpperCase());
                } catch (IllegalArgumentException e) {
                    TextComponent msg = new TextComponent(ChatColor.RED + "Invalid generator type! Use: IRON, GOLD, DIAMOND, EMERALD");
                    msg.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(ChatColor.GRAY + "Click for help")));
                    msg.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/bw help"));
                    player.spigot().sendMessage(msg);
                    return true;
                }

                Optional<Arena> targetArena;
                if (arenaName != null) {
                    targetArena = arenaManager.getArenaByName(arenaName);
                } else {
                    targetArena = arenaManager.getArenaByLocation(player.getLocation());
                }

                targetArena.ifPresentOrElse(arena -> {
                    org.bukkit.Location loc = player.getLocation();
                    Generator toRemove = null;
                    double nearestDistSq = 100.0; // Search radius 10 blocks (10^2)

                    for (Generator gen : arena.getGenerators()) {
                        if (gen.getType() == type) {
                            double distSq = gen.getLocation().distanceSquared(loc);
                            if (distSq < nearestDistSq) {
                                nearestDistSq = distSq;
                                toRemove = gen;
                            }
                        }
                    }

                    if (toRemove != null) {
                        toRemove.remove(); // Remove hologram
                        arena.getGenerators().remove(toRemove);
                        new ArenaConfig(arena.getArenaName()).saveArena(arena);
                        TextComponent success = new TextComponent(ChatColor.GREEN + type.name() + " Generator removed from arena " + arena.getArenaName() + "!");
                        success.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(ChatColor.GRAY + "Click to check setup status")));
                        success.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/bw setupArena " + arena.getArenaName()));
                        player.spigot().sendMessage(success);
                        sendSetupStatus(player, arena);
                    } else {
                        TextComponent msg = new TextComponent(ChatColor.RED + "No " + type.name() + " generator found within 10 blocks!");
                        msg.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(ChatColor.GRAY + "Stand closer to the generator.")));
                        player.spigot().sendMessage(msg);
                    }
                }, () -> {
                    TextComponent error = new TextComponent(ChatColor.RED + "Arena not found! Stand near the arena or specify its name.");
                    error.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(ChatColor.GRAY + "Click to list arenas")));
                    error.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/bw list"));
                    player.spigot().sendMessage(error);
                });
                break;
            }

            case "removeshop": {
                // Flexible: /bw removeshop [arena] <team>
                String shopArenaName = null;
                String shopTeamName = null;

                if (args.length == 2) {
                    shopTeamName = args[1];
                } else if (args.length >= 3) {
                    shopArenaName = args[1];
                    shopTeamName = args[2];
                } else {
                    sendUsage(player, "/bw removeshop [arena] <team>");
                    return true;
                }

                Optional<Arena> shopArena;
                if (shopArenaName != null) {
                    shopArena = arenaManager.getArenaByName(shopArenaName);
                } else {
                    shopArena = arenaManager.getArenaByLocation(player.getLocation());
                }

                String finalShopTeamName = shopTeamName;
                shopArena.ifPresentOrElse(arena -> {
                    arena.getTeams().stream().filter(t -> t.getName().equalsIgnoreCase(finalShopTeamName)).findFirst().ifPresentOrElse(team -> {
                        // Remove the NPC using SpyNPCs (UUID-based)
                        java.util.UUID uuid = java.util.UUID.nameUUIDFromBytes((arena.getArenaName() + "shop" + team.getName()).getBytes());
                        SpyBedWars.getInstance().getNpcManager().removeNPC(uuid);

                        team.setShopNPC(null);
                        new ArenaConfig(arena.getArenaName()).saveArena(arena);
                        TextComponent success = new TextComponent(ChatColor.GREEN + "Shop NPC removed for team " + finalShopTeamName + " in arena " + arena.getArenaName());
                        success.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(ChatColor.GRAY + "Click to set shop NPC")));
                        success.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/bw setshop " + arena.getArenaName() + " " + finalShopTeamName));
                        player.spigot().sendMessage(success);
                        sendSetupStatus(player, arena);
                    }, () -> {
                        TextComponent msg = new TextComponent(ChatColor.RED + "Team " + finalShopTeamName + " not found in arena " + arena.getArenaName());
                        msg.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(ChatColor.GRAY + "Check team list in /bw setupArena")));
                        player.spigot().sendMessage(msg);
                    });
                }, () -> {
                    TextComponent error = new TextComponent(ChatColor.RED + "Arena not found!");
                    error.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(ChatColor.GRAY + "Click to list arenas")));
                    error.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/bw list"));
                    player.spigot().sendMessage(error);
                });
                break;
            }

            case "removeupgrade": {
                // Flexible: /bw removeupgrade [arena] <team>
                String upgradeArenaName = null;
                String upgradeTeamName = null;

                if (args.length == 2) {
                    upgradeTeamName = args[1];
                } else if (args.length >= 3) {
                    upgradeArenaName = args[1];
                    upgradeTeamName = args[2];
                } else {
                    sendUsage(player, "/bw removeupgrade [arena] <team>");
                    return true;
                }

                Optional<Arena> upgradeArena;
                if (upgradeArenaName != null) {
                    upgradeArena = arenaManager.getArenaByName(upgradeArenaName);
                } else {
                    upgradeArena = arenaManager.getArenaByLocation(player.getLocation());
                }

                String finalUpgradeTeamName = upgradeTeamName;
                upgradeArena.ifPresentOrElse(arena -> {
                    arena.getTeams().stream().filter(t -> t.getName().equalsIgnoreCase(finalUpgradeTeamName)).findFirst().ifPresentOrElse(team -> {
                        // Remove the NPC using SpyNPCs (UUID-based)
                        java.util.UUID uuid = java.util.UUID.nameUUIDFromBytes((arena.getArenaName() + "upgrade" + team.getName()).getBytes());
                        SpyBedWars.getInstance().getNpcManager().removeNPC(uuid);

                        team.setUpgradeNPC(null);
                        new ArenaConfig(arena.getArenaName()).saveArena(arena);
                        TextComponent success = new TextComponent(ChatColor.GREEN + "Upgrade NPC removed for team " + finalUpgradeTeamName + " in arena " + arena.getArenaName());
                        success.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(ChatColor.GRAY + "Click to set upgrade NPC")));
                        success.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/bw setupgrade " + arena.getArenaName() + " " + finalUpgradeTeamName));
                        player.spigot().sendMessage(success);
                        sendSetupStatus(player, arena);
                    }, () -> {
                        TextComponent msg = new TextComponent(ChatColor.RED + "Team " + finalUpgradeTeamName + " not found in arena " + arena.getArenaName());
                        msg.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(ChatColor.GRAY + "Check team list in /bw setupArena")));
                        player.spigot().sendMessage(msg);
                    });
                }, () -> {
                    TextComponent error = new TextComponent(ChatColor.RED + "Arena not found!");
                    error.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(ChatColor.GRAY + "Click to list arenas")));
                    error.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/bw list"));
                    player.spigot().sendMessage(error);
                });
                break;
            }

            case "setlobby":
                SpyBedWars.getInstance().getConfig().set("main-lobby", com.spygamingog.spybedwars.utils.LocationUtil.serialize(player.getLocation()));
                SpyBedWars.getInstance().saveConfig();
                TextComponent lobbyMsg = new TextComponent(getMessage("setup.lobby-set"));
                lobbyMsg.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(ChatColor.GRAY + "Click to see all arenas")));
                lobbyMsg.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/bw list"));
                player.spigot().sendMessage(lobbyMsg);
                
                // Enable flight for the player who set the lobby
                player.setAllowFlight(true);
                player.setFlying(true);
                break;

            case "settype": {
                // Flexible: /bw settype [arena] <SOLO|DOUBLES|TRIPLES|FOURS>
                String setTypeArenaName = null;
                String setTypeName = null;

                if (args.length == 2) {
                    setTypeName = args[1];
                } else if (args.length >= 3) {
                    setTypeArenaName = args[1];
                    setTypeName = args[2];
                } else {
                    sendUsage(player, "/bw settype [arena] <SOLO|DOUBLES|TRIPLES|FOURS>");
                    return true;
                }

                ArenaType type;
                try {
                    type = ArenaType.valueOf(setTypeName.toUpperCase());
                } catch (IllegalArgumentException e) {
                    TextComponent msg = new TextComponent(ChatColor.RED + "Invalid arena type! Use: SOLO, DOUBLES, TRIPLES, FOURS");
                    msg.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(ChatColor.GRAY + "Click for help")));
                    msg.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/bw help"));
                    player.spigot().sendMessage(msg);
                    return true;
                }

                Optional<Arena> setTypeArena;
                if (setTypeArenaName != null) {
                    setTypeArena = arenaManager.getArenaByName(setTypeArenaName);
                } else {
                    setTypeArena = arenaManager.getArenaByLocation(player.getLocation());
                }

                setTypeArena.ifPresentOrElse(arena -> {
                    arena.setArenaType(type);
                    new ArenaConfig(arena.getArenaName()).saveArena(arena);
                    TextComponent success = new TextComponent(getMessage("setup.type-set").replace("{type}", type.name()).replace("{arena}", arena.getArenaName()));
                    success.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(ChatColor.GRAY + "Click to check setup status")));
                    success.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/bw setupArena " + arena.getArenaName()));
                    player.spigot().sendMessage(success);
                    sendSetupStatus(player, arena);
                }, () -> {
                    TextComponent error = new TextComponent(ChatColor.RED + "Arena not found! Stand near the arena or specify its name.");
                    error.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(ChatColor.GRAY + "Click to list arenas")));
                    error.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/bw list"));
                    player.spigot().sendMessage(error);
                });
                break;
            }

            case "setmin": {
                // Flexible: /bw setmin [arena] <count>
                String setMinArenaName = null;
                String setMinCountStr = null;

                if (args.length == 2) {
                    setMinCountStr = args[1];
                } else if (args.length >= 3) {
                    setMinArenaName = args[1];
                    setMinCountStr = args[2];
                } else {
                    sendUsage(player, "/bw setmin [arena] <count>");
                    return true;
                }

                int min;
                try {
                    min = Integer.parseInt(setMinCountStr);
                } catch (NumberFormatException e) {
                    TextComponent msg = new TextComponent(ChatColor.RED + "Invalid number!");
                    msg.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(ChatColor.GRAY + "Please enter a valid integer.")));
                    msg.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/bw help"));
                    player.spigot().sendMessage(msg);
                    return true;
                }

                Optional<Arena> setMinArena;
                if (setMinArenaName != null) {
                    setMinArena = arenaManager.getArenaByName(setMinArenaName);
                } else {
                    setMinArena = arenaManager.getArenaByLocation(player.getLocation());
                }

                String finalSetMinCountStr = setMinCountStr;
                setMinArena.ifPresentOrElse(arena -> {
                    arena.setMinPlayers(min);
                    new ArenaConfig(arena.getArenaName()).saveArena(arena);
                    TextComponent success = new TextComponent(getMessage("setup.min-players-set").replace("{count}", finalSetMinCountStr).replace("{arena}", arena.getArenaName()));
                    success.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(ChatColor.GRAY + "Click to check setup status")));
                    success.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/bw setupArena " + arena.getArenaName()));
                    player.spigot().sendMessage(success);
                    sendSetupStatus(player, arena);
                }, () -> {
                    TextComponent error = new TextComponent(ChatColor.RED + "Arena not found! Stand near the arena or specify its name.");
                    error.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(ChatColor.GRAY + "Click to list arenas")));
                    error.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/bw list"));
                    player.spigot().sendMessage(error);
                });
                break;
            }

            case "setmax": {
                // Flexible: /bw setmax [arena] <count>
                String setMaxArenaName = null;
                String setMaxCountStr = null;

                if (args.length == 2) {
                    setMaxCountStr = args[1];
                } else if (args.length >= 3) {
                    setMaxArenaName = args[1];
                    setMaxCountStr = args[2];
                } else {
                    sendUsage(player, "/bw setmax [arena] <count>");
                    return true;
                }

                int max;
                try {
                    max = Integer.parseInt(setMaxCountStr);
                } catch (NumberFormatException e) {
                    TextComponent msg = new TextComponent(ChatColor.RED + "Invalid number!");
                    msg.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(ChatColor.GRAY + "Please enter a valid integer.")));
                    msg.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/bw help"));
                    player.spigot().sendMessage(msg);
                    return true;
                }

                Optional<Arena> setMaxArena;
                if (setMaxArenaName != null) {
                    setMaxArena = arenaManager.getArenaByName(setMaxArenaName);
                } else {
                    setMaxArena = arenaManager.getArenaByLocation(player.getLocation());
                }

                String finalSetMaxCountStr = setMaxCountStr;
                setMaxArena.ifPresentOrElse(arena -> {
                    arena.setMaxPlayers(max);
                    new ArenaConfig(arena.getArenaName()).saveArena(arena);
                    TextComponent success = new TextComponent(getMessage("setup.max-players-set").replace("{count}", finalSetMaxCountStr).replace("{arena}", arena.getArenaName()));
                    success.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(ChatColor.GRAY + "Click to check setup status")));
                    success.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/bw setupArena " + arena.getArenaName()));
                    player.spigot().sendMessage(success);
                    sendSetupStatus(player, arena);
                }, () -> {
                    TextComponent error = new TextComponent(ChatColor.RED + "Arena not found! Stand near the arena or specify its name.");
                    error.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(ChatColor.GRAY + "Click to list arenas")));
                    error.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/bw list"));
                    player.spigot().sendMessage(error);
                });
                break;
            }

            case "setshop": {
                player.sendMessage(ChatColor.GRAY + "[Debug] setshop command received.");
                // Flexible: /bw setshop [arena] <team>
                String setShopArenaName = null;
                String setShopTeamName = null;

                if (args.length == 2) {
                    setShopTeamName = args[1];
                } else if (args.length >= 3) {
                    setShopArenaName = args[1];
                    setShopTeamName = args[2];
                } else {
                    sendUsage(player, "/bw setshop [arena] <team>");
                    return true;
                }

                TeamColor shopColor;
                try {
                    shopColor = TeamColor.valueOf(setShopTeamName.toUpperCase());
                } catch (IllegalArgumentException e) {
                    TextComponent msg = new TextComponent(ChatColor.RED + "Invalid team color! Use one of: " + 
                        Arrays.stream(TeamColor.values()).map(Enum::name).collect(Collectors.joining(", ")));
                    msg.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(ChatColor.GRAY + "Click for help")));
                    msg.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/bw help"));
                    player.spigot().sendMessage(msg);
                    return true;
                }

                Optional<Arena> setShopArena;
                if (setShopArenaName != null) {
                    setShopArena = arenaManager.getArenaByName(setShopArenaName);
                } else {
                    setShopArena = arenaManager.getArenaByLocation(player.getLocation());
                }

                setShopArena.ifPresentOrElse(arena -> {
                    String finalTeamName = shopColor.name();
                    Team team = arena.getTeams().stream()
                            .filter(t -> t.getName().equalsIgnoreCase(finalTeamName))
                            .findFirst()
                            .orElseGet(() -> {
                                Team newTeam = new Team(finalTeamName, shopColor);
                                arena.getTeams().add(newTeam);
                                return newTeam;
                            });

                    team.setShopNPC(player.getLocation());
                    new ArenaConfig(arena.getArenaName()).saveArena(arena);
                    
                    player.sendMessage(ChatColor.YELLOW + "Attempting to create NPC...");

                    // Spawn NPC immediately for visibility
                    SpyBedWars.getInstance().getNpcManager().spawnShopNPC(
                        player.getLocation(),
                        arena.getArenaName(),
                        finalTeamName
                    );

                    TextComponent success = new TextComponent(getMessage("setup.shop-set").replace("{team}", finalTeamName).replace("{arena}", arena.getArenaName()));
                    success.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(ChatColor.GRAY + "Click to set upgrade NPC")));
                    success.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/bw setupgrade " + arena.getArenaName() + " " + finalTeamName));
                    player.spigot().sendMessage(success);
                    sendSetupStatus(player, arena);
                }, () -> {
                    TextComponent error = new TextComponent(ChatColor.RED + "Arena not found! Stand near the arena or specify its name.");
                    error.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(ChatColor.GRAY + "Click to list arenas")));
                    error.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/bw list"));
                    player.spigot().sendMessage(error);
                });
                break;
            }

            case "setupgrade": {
                player.sendMessage(ChatColor.GRAY + "[Debug] setupgrade command received.");
                // Flexible: /bw setupgrade [arena] <team>
                String setUpgradeArenaName = null;
                String setUpgradeTeamName = null;

                if (args.length == 2) {
                    setUpgradeTeamName = args[1];
                } else if (args.length >= 3) {
                    setUpgradeArenaName = args[1];
                    setUpgradeTeamName = args[2];
                } else {
                    sendUsage(player, "/bw setupgrade [arena] <team>");
                    return true;
                }

                TeamColor upgradeColor;
                try {
                    upgradeColor = TeamColor.valueOf(setUpgradeTeamName.toUpperCase());
                } catch (IllegalArgumentException e) {
                    TextComponent msg = new TextComponent(ChatColor.RED + "Invalid team color! Use one of: " + 
                        Arrays.stream(TeamColor.values()).map(Enum::name).collect(Collectors.joining(", ")));
                    msg.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(ChatColor.GRAY + "Click for help")));
                    msg.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/bw help"));
                    player.spigot().sendMessage(msg);
                    return true;
                }

                Optional<Arena> setUpgradeArena;
                if (setUpgradeArenaName != null) {
                    setUpgradeArena = arenaManager.getArenaByName(setUpgradeArenaName);
                } else {
                    setUpgradeArena = arenaManager.getArenaByLocation(player.getLocation());
                }

                setUpgradeArena.ifPresentOrElse(arena -> {
                    String finalTeamName = upgradeColor.name();
                    Team team = arena.getTeams().stream()
                            .filter(t -> t.getName().equalsIgnoreCase(finalTeamName))
                            .findFirst()
                            .orElseGet(() -> {
                                Team newTeam = new Team(finalTeamName, upgradeColor);
                                arena.getTeams().add(newTeam);
                                return newTeam;
                            });

                    team.setUpgradeNPC(player.getLocation());
                    new ArenaConfig(arena.getArenaName()).saveArena(arena);

                    player.sendMessage(ChatColor.YELLOW + "Attempting to create NPC...");
                    
                    // Spawn NPC immediately for visibility
                    SpyBedWars.getInstance().getNpcManager().spawnUpgradeNPC(
                        player.getLocation(),
                        arena.getArenaName(),
                        finalTeamName
                    );

                    TextComponent success = new TextComponent(getMessage("setup.upgrade-set").replace("{team}", finalTeamName).replace("{arena}", arena.getArenaName()));
                    success.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(ChatColor.GRAY + "Click to check setup status")));
                    success.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/bw setupArena " + arena.getArenaName()));
                    player.spigot().sendMessage(success);
                    sendSetupStatus(player, arena);
                }, () -> {
                    TextComponent error = new TextComponent(ChatColor.RED + "Arena not found! Stand near the arena or specify its name.");
                    error.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(ChatColor.GRAY + "Click to list arenas")));
                    error.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/bw list"));
                    player.spigot().sendMessage(error);
                });
                break;
            }

            case "setspectator": {
                // Flexible: /bw setspectator [arena]
                String setSpectatorArenaName = null;
                if (args.length >= 2) {
                    setSpectatorArenaName = args[1];
                }

                Optional<Arena> setSpectatorArena;
                if (setSpectatorArenaName != null) {
                    setSpectatorArena = arenaManager.getArenaByName(setSpectatorArenaName);
                } else {
                    setSpectatorArena = arenaManager.getArenaByLocation(player.getLocation());
                }

                setSpectatorArena.ifPresentOrElse(arena -> {
                    arena.setSpectatorLocation(player.getLocation());
                    new ArenaConfig(arena.getArenaName()).saveArena(arena);
                    TextComponent success = new TextComponent(getMessage("setup.spectator-set").replace("{arena}", arena.getArenaName()));
                    success.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(ChatColor.GRAY + "Click to check setup status")));
                    success.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/bw setupArena " + arena.getArenaName()));
                    player.spigot().sendMessage(success);
                    sendSetupStatus(player, arena);
                }, () -> {
                    TextComponent error = new TextComponent(ChatColor.RED + "Arena not found! Stand near the arena or specify its name.");
                    error.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(ChatColor.GRAY + "Click to list arenas")));
                    error.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/bw list"));
                    player.spigot().sendMessage(error);
                });
                break;
            }

            case "lobby": {
                if (args.length >= 2 && args[1].equalsIgnoreCase("onjoin")) {
                    if (args.length >= 3) {
                        boolean enable = args[2].equalsIgnoreCase("enable");
                        SpyBedWars.getInstance().getConfig().set("lobby-on-join", enable);
                        SpyBedWars.getInstance().saveConfig();
                        TextComponent msg = new TextComponent(ChatColor.GREEN + "Lobby on join " + (enable ? "enabled" : "disabled") + "!");
                        msg.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(ChatColor.GRAY + "Players will " + (enable ? "now" : "no longer") + " spawn at lobby on join.")));
                        player.spigot().sendMessage(msg);
                    } else {
                        sendUsage(player, "/bw lobby onjoin <enable|disable>");
                    }
                    return true;
                }

                Location lobby = arenaManager.getLobbyLocation();
                if (lobby != null) {
                    player.teleport(lobby);
                    player.setGameMode(org.bukkit.GameMode.ADVENTURE);
                    player.setAllowFlight(true);
                    player.setFlying(true);
                    SpyBedWars.getInstance().getLobbyItemManager().giveLobbyItems(player);
                    player.sendMessage(ChatColor.GREEN + "Teleported to lobby!");
                } else {
                    player.sendMessage(ChatColor.RED + "Lobby not set! Use /bw setlobby first.");
                }
                break;
            }

            case "shop":
                    SpyBedWars.getInstance().getShopManager().openShop(player, ShopCategory.BLOCKS);
                    break;

                case "npcshop":
                    // Internal command triggered by NPC interaction
                    SpyBedWars.getInstance().getShopManager().openShop(player, ShopCategory.BLOCKS);
                    break;

                case "upgrade":
                case "upgrades":
                    arenaManager.getArenaByPlayer(player).ifPresentOrElse(arena -> {
                        Team team = arena.getTeam(player);
                        if (team != null) {
                            SpyBedWars.getInstance().getUpgradeManager().openUpgrades(player, team);
                        } else {
                            TextComponent msg = new TextComponent(ChatColor.RED + "You are not in a team!");
                            msg.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(ChatColor.GRAY + "You must be in a team to see upgrades.")));
                            msg.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/bw help"));
                            player.spigot().sendMessage(msg);
                        }
                    }, () -> {
                        TextComponent msg = new TextComponent(ChatColor.RED + "You must be in a game to use this command!");
                        msg.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(ChatColor.GRAY + "Click to join an arena")));
                        msg.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/bw list"));
                        player.spigot().sendMessage(msg);
                    });
                    break;

                case "npcupgrade":
                    // Internal command triggered by NPC interaction
                    arenaManager.getArenaByPlayer(player).ifPresentOrElse(arena -> {
                        Team team = arena.getTeam(player);
                        if (team != null) {
                            SpyBedWars.getInstance().getUpgradeManager().openUpgrades(player, team);
                        }
                    }, () -> {
                        player.sendMessage(ChatColor.RED + "You must be in a game to use upgrades!");
                    });
                    break;

            case "gui":
                if (args.length == 1) {
                    SpyBedWars.getInstance().getMenuManager().openMainMenu(player);
                } else {
                    String mode = args[1].toUpperCase();
                    try {
                        ArenaType type = ArenaType.valueOf(mode);
                        SpyBedWars.getInstance().getMenuManager().openModeMenu(player, type);
                    } catch (IllegalArgumentException e) {
                        player.sendMessage(ChatColor.RED + "Invalid mode! Use: SOLO, DOUBLES, TRIPLES, or FOURS");
                    }
                }
                break;

            default:
                sendHelp(player);
                break;
        }

        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (!(sender instanceof Player)) return null;

        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            String[] subCommands = {
                "create", "list", "delete", "enable", "disable", "clone", "setupArena", "setup",
                "setwaiting", "setspectator", "settype", "setmin", "setmax", "setspawn",
                "setbed", "removebed", "setshop", "setupgrade", "removeshop", "removeupgrade",
                "addgenerator", "removegenerator", "join", "setlobby", "lobby", "shop", "upgrade"
            };
            for (String sub : subCommands) {
                if (sub.toLowerCase().startsWith(args[0].toLowerCase())) {
                    completions.add(sub);
                }
            }
        } else if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if (Arrays.asList("delete", "enable", "disable", "clone", "setuparena", "setup", "setwaiting", "setspectator", "settype", "setmin", "setmax", "setspawn", "setbed", "removebed", "setshop", "setupgrade", "removeshop", "removeupgrade", "addgenerator", "removegenerator", "join").contains(sub)) {
                // Suggest Arenas
                completions.addAll(arenaManager.getArenas().stream()
                    .map(Arena::getArenaName)
                    .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList()));
                
                // For NPC commands, also suggest Team Colors if the argument doesn't match an arena yet
                if (Arrays.asList("setshop", "setupgrade", "removeshop", "removeupgrade", "setspawn", "setbed", "removebed").contains(sub)) {
                    for (TeamColor color : TeamColor.values()) {
                        if (color.name().toLowerCase().startsWith(args[1].toLowerCase())) {
                            completions.add(color.name());
                        }
                    }
                }

                // For Generator commands, also suggest Generator Types
                if (Arrays.asList("addgenerator", "removegenerator").contains(sub)) {
                    for (GeneratorType type : GeneratorType.values()) {
                        if (type.name().toLowerCase().startsWith(args[1].toLowerCase())) {
                            completions.add(type.name());
                        }
                    }
                }
            } else if (sub.equals("lobby")) {
                if ("onjoin".startsWith(args[1].toLowerCase())) {
                    completions.add("onjoin");
                }
            }
        } else if (args.length == 3) {
            String sub = args[0].toLowerCase();
            if (sub.equals("lobby") && args[1].equalsIgnoreCase("onjoin")) {
                if ("enable".startsWith(args[2].toLowerCase())) completions.add("enable");
                if ("disable".startsWith(args[2].toLowerCase())) completions.add("disable");
            } else if (sub.equals("setup")) {
                if ("generators".startsWith(args[2].toLowerCase())) {
                    completions.add("generators");
                }
            } else if (Arrays.asList("setspawn", "setbed", "removebed", "setshop", "setupgrade", "addteam", "removeshop", "removeupgrade").contains(sub)) {
                for (TeamColor color : TeamColor.values()) {
                    if (color.name().toLowerCase().startsWith(args[2].toLowerCase())) {
                        completions.add(color.name());
                    }
                }
            } else if (sub.equals("addgenerator") || sub.equals("removegenerator")) {
                for (GeneratorType type : GeneratorType.values()) {
                    if (type.name().toLowerCase().startsWith(args[2].toLowerCase())) {
                        completions.add(type.name());
                    }
                }
            } else if (sub.equals("settype")) {
                for (ArenaType type : ArenaType.values()) {
                    if (type.name().toLowerCase().startsWith(args[2].toLowerCase())) {
                        completions.add(type.name());
                    }
                }
            } else if (sub.equals("clone")) {
                completions.add("<new_name>");
            }
        } else if (args.length == 4) {
            String sub = args[0].toLowerCase();
            if (Arrays.asList("setspawn", "setbed", "removebed", "setshop", "setupgrade", "removeshop", "removeupgrade").contains(sub)) {
                for (TeamColor color : TeamColor.values()) {
                    if (color.name().toLowerCase().startsWith(args[3].toLowerCase())) {
                        completions.add(color.name());
                    }
                }
            }
        }

        return completions;
    }

    private void sendMessage(Player player, String path) {
        TextComponent msg = new TextComponent(getMessage(path));
        msg.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(ChatColor.GRAY + "SpyBedWars Notification")));
        msg.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/bw help"));
        player.spigot().sendMessage(msg);
    }

    private String getMessage(String path) {
        String prefix = SpyBedWars.getInstance().getMessagesConfig().getString("prefix", "&8[&bSpyBedWars&8] ");
        String msg = SpyBedWars.getInstance().getMessagesConfig().getString(path, path);
        return ChatColor.translateAlternateColorCodes('&', prefix + msg);
    }

    private void sendUsage(Player player, String usage) {
        TextComponent msg = new TextComponent(ChatColor.RED + "Usage: " + usage);
        msg.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(ChatColor.GRAY + "Click for help")));
        msg.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/bw help"));
        player.spigot().sendMessage(msg);
    }

    private void sendHelp(Player player) {
        TextComponent header = new TextComponent(ChatColor.GOLD + "--- SpyBedWars Help ---");
        header.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(ChatColor.YELLOW + "Click to visit our discord!")));
        header.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, "https://discord.gg/spygaming")); // Placeholder URL
        player.spigot().sendMessage(header);
        
        sendClickableCommand(player, "/bw join <arena>", "Join an arena", "/bw join ", false);
        sendClickableCommand(player, "/bw list", "List all arenas", "/bw list", true);
        sendClickableCommand(player, "/bw leave", "Leave your current game", "/bw leave", true);
        
        if (player.hasPermission("spybedwars.admin")) {
            TextComponent adminHeader = new TextComponent(ChatColor.GRAY + "Admin Commands:");
            adminHeader.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(ChatColor.GRAY + "Only visible to administrators.")));
            player.spigot().sendMessage(adminHeader);
            sendClickableCommand(player, "/bw create <name>", "Create a new arena", "/bw create ", false);
            sendClickableCommand(player, "/bw delete <arena>", "Delete an arena", "/bw delete ", false);
            sendClickableCommand(player, "/bw enable <arena>", "Enable an arena", "/bw enable ", false);
            sendClickableCommand(player, "/bw disable <arena>", "Disable an arena", "/bw disable ", false);
            sendClickableCommand(player, "/bw setupArena <arena>", "Check arena setup status", "/bw setupArena ", false);
            sendClickableCommand(player, "/bw setwaiting <arena>", "Set the waiting lobby", "/bw setwaiting ", false);
            sendClickableCommand(player, "/bw setspectator <arena>", "Set spectator spawn", "/bw setspectator ", false);
            sendClickableCommand(player, "/bw settype <arena> <type>", "Set arena type", "/bw settype ", false);
            sendClickableCommand(player, "/bw setmin <arena> <count>", "Set min players", "/bw setmin ", false);
            sendClickableCommand(player, "/bw setmax <arena> <count>", "Set max players", "/bw setmax ", false);
            sendClickableCommand(player, "/bw setspawn <arena> <team>", "Set team spawn", "/bw setspawn ", false);
            sendClickableCommand(player, "/bw setbed <arena> <team>", "Set team bed location", "/bw setbed ", false);
            sendClickableCommand(player, "/bw setshop <arena> <team>", "Set Shop NPC location", "/bw setshop ", false);
            sendClickableCommand(player, "/bw setupgrade <arena> <team>", "Set Upgrade NPC location", "/bw setupgrade ", false);
            sendClickableCommand(player, "/bw addgenerator <arena> <type>", "Add a generator", "/bw addgenerator ", false);
            sendClickableCommand(player, "/bw removegenerator <arena>", "Remove nearest generator", "/bw removegenerator ", false);
            sendClickableCommand(player, "/bw setlobby", "Set main lobby", "/bw setlobby", true);
            sendClickableCommand(player, "/bw lobby", "Teleport to lobby", "/bw lobby", true);
            sendClickableCommand(player, "/bw lobby onjoin <enable|disable>", "Toggle spawn at lobby on join", "/bw lobby onjoin ", false);
            sendClickableCommand(player, "/bw clone <arena> <new_name>", "Clone an arena", "/bw clone ", false);
        }
    }

    private void sendClickableCommand(Player player, String command, String description, String suggestion, boolean execute) {
        TextComponent message = new TextComponent(ChatColor.YELLOW + command + ChatColor.GRAY + " - " + description);
        String hoverText = execute ? ChatColor.AQUA + "Click to run: " : ChatColor.AQUA + "Click to suggest: ";
        message.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(hoverText + ChatColor.YELLOW + command)));
        message.setClickEvent(new ClickEvent(execute ? ClickEvent.Action.RUN_COMMAND : ClickEvent.Action.SUGGEST_COMMAND, suggestion));
        player.spigot().sendMessage(message);
    }

    private void sendRequirementStatus(Player player, boolean condition, String label, String command) {
        TextComponent status = new TextComponent((condition ? ChatColor.GREEN + "✔" : ChatColor.RED + "✗") + ChatColor.YELLOW + " " + label);
        status.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(ChatColor.GRAY + "Click to set " + label)));
        status.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command));
        player.spigot().sendMessage(status);
    }

    private TextComponent createStatusPart(boolean condition, String label, String description, String setCommand, String removeCommand) {
        TextComponent part = new TextComponent((condition ? ChatColor.GREEN : ChatColor.RED) + label);
        String hoverText = condition ? ChatColor.GRAY + "Click to remove " + description : ChatColor.GRAY + "Click to set " + description;
        String command = condition ? removeCommand : setCommand;
        part.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(hoverText)));
        part.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command));
        return part;
    }

    private void sendSetupStatus(Player player, Arena arena) {
        TextComponent title = new TextComponent(ChatColor.GOLD + "--- Setup Status: " + arena.getArenaName() + " ---");
        title.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(ChatColor.GRAY + "Overview of arena configuration.")));
        player.spigot().sendMessage(title);

        sendRequirementStatus(player, arena.getWaitingLobby() != null, "Waiting Lobby", "/bw setwaiting " + arena.getArenaName());
        sendRequirementStatus(player, arena.getSpectatorLocation() != null, "Spectator Location", "/bw setspectator " + arena.getArenaName());
        sendRequirementStatus(player, !arena.getTeams().isEmpty(), "Teams Configured (" + arena.getTeams().size() + ")", "/bw addteam " + arena.getArenaName() + " ");
        sendRequirementStatus(player, !arena.getGenerators().isEmpty(), "Generators Configured (" + arena.getGenerators().size() + ")", "/bw setup " + arena.getArenaName() + " generators");

        for (TeamColor color : TeamColor.values()) {
            Optional<Team> teamOpt = arena.getTeams().stream().filter(t -> t.getColor() == color).findFirst();

            TextComponent teamMsg = new TextComponent(color.getChatColor() + "Team " + color.name() + ": ");

            if (teamOpt.isPresent()) {
                Team team = teamOpt.get();
                teamMsg.addExtra(createStatusPart(team.getSpawnPoint() != null, "S", "Spawn", 
                    "/bw setspawn " + arena.getArenaName() + " " + team.getName(), 
                    "/bw removespawn " + arena.getArenaName() + " " + team.getName()));
                teamMsg.addExtra(" ");
                teamMsg.addExtra(createStatusPart(team.getBedLocation() != null, "B", "Bed", 
                    "/bw setbed " + arena.getArenaName() + " " + team.getName(), 
                    "/bw removebed " + arena.getArenaName() + " " + team.getName()));
                teamMsg.addExtra(" ");
                teamMsg.addExtra(createStatusPart(team.getShopNPC() != null, "N", "Shop NPC", 
                    "/bw setshop " + arena.getArenaName() + " " + team.getName(), 
                    "/bw removeshop " + arena.getArenaName() + " " + team.getName()));
                teamMsg.addExtra(" ");
                teamMsg.addExtra(createStatusPart(team.getUpgradeNPC() != null, "U", "Upgrade NPC", 
                    "/bw setupgrade " + arena.getArenaName() + " " + team.getName(), 
                    "/bw removeupgrade " + arena.getArenaName() + " " + team.getName()));
            } else {
                // Team not yet added, show all red and click to add team
                String addCmd = "/bw addteam " + arena.getArenaName() + " " + color.name();
                teamMsg.addExtra(createStatusPart(false, "S", "Spawn (Add Team First)", addCmd, addCmd));
                teamMsg.addExtra(" ");
                teamMsg.addExtra(createStatusPart(false, "B", "Bed (Add Team First)", addCmd, addCmd));
                teamMsg.addExtra(" ");
                teamMsg.addExtra(createStatusPart(false, "N", "Shop NPC (Add Team First)", addCmd, addCmd));
                teamMsg.addExtra(" ");
                teamMsg.addExtra(createStatusPart(false, "U", "Upgrade NPC (Add Team First)", addCmd, addCmd));
            }

            player.spigot().sendMessage(teamMsg);
        }

        if (arena.canEnable()) {
            TextComponent readyMsg = new TextComponent(ChatColor.GREEN + "Arena is ready to be enabled!");
            readyMsg.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(ChatColor.GRAY + "Click to enable")));
            readyMsg.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/bw enable " + arena.getArenaName()));
            player.spigot().sendMessage(readyMsg);
        } else {
            TextComponent error = new TextComponent(ChatColor.RED + "Arena is missing requirements!");
            error.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(ChatColor.GRAY + "Check the list above for missing parts.")));
            player.spigot().sendMessage(error);
        }
    }

    private boolean isIsolated(org.bukkit.block.Block block, org.bukkit.Material type) {
        // Check surrounding blocks (including diagonals) for the same type
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    if (x == 0 && y == 0 && z == 0) continue;
                    if (block.getRelative(x, y, z).getType() == type) {
                        return false;
                    }
                }
            }
        }
        return true;
    }
}
