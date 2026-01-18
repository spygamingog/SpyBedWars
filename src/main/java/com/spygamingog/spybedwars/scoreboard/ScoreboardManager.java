package com.spygamingog.spybedwars.scoreboard;

import com.spygamingog.spybedwars.SpyBedWars;
import com.spygamingog.spybedwars.api.arena.GameState;
import com.spygamingog.spybedwars.arena.Arena;
import com.spygamingog.spybedwars.arena.team.Team;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.*;

import java.util.ArrayList;
import java.util.List;

public class ScoreboardManager {
    private final java.util.Map<java.util.UUID, Scoreboard> scoreboards = new java.util.HashMap<>();
    private final java.util.Map<java.util.UUID, List<String>> lastLines = new java.util.HashMap<>();

    public void updateScoreboard(Player player, Arena arena) {
        Scoreboard scoreboard = scoreboards.computeIfAbsent(player.getUniqueId(), k -> Bukkit.getScoreboardManager().getNewScoreboard());
        Objective objective = scoreboard.getObjective("bedwars");
        
        if (objective == null) {
            objective = scoreboard.registerNewObjective("bedwars", Criteria.DUMMY, ChatColor.YELLOW + "" + ChatColor.BOLD + "BED WARS");
            objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        }

        List<String> lines = new ArrayList<>();
        if (arena.getGameState() == GameState.WAITING || arena.getGameState() == GameState.STARTING) {
            lines.add(ChatColor.GRAY + "01/11/26 " + ChatColor.DARK_GRAY + "m123");
            lines.add("");
            String mapName = arena.getArenaName();
            if (arena.getWaitingLobby() != null) {
                String fullPath = null;
                try {
                    fullPath = arena.getWaitingLobby().getWorld().getName();
                } catch (IllegalArgumentException e) {
                    // World unloaded, try extract from toString
                    String locStr = arena.getWaitingLobby().toString();
                    if (locStr.contains("world=")) {
                        int start = locStr.indexOf("world=") + 6;
                        int end = locStr.indexOf(",", start);
                        if (end != -1) fullPath = locStr.substring(start, end);
                    }
                }

                if (fullPath != null) {
                    if (fullPath.contains("/")) {
                        mapName = fullPath.substring(fullPath.lastIndexOf("/") + 1);
                    } else if (fullPath.contains("\\")) {
                        mapName = fullPath.substring(fullPath.lastIndexOf("\\") + 1);
                    } else {
                        mapName = fullPath;
                    }
                }
            }
            lines.add("Map: " + ChatColor.GREEN + mapName);
            lines.add("Players: " + ChatColor.GREEN + arena.getPlayers().size() + "/" + arena.getMaxPlayers());
            lines.add("");
            if (arena.getGameState() == GameState.STARTING) {
                lines.add("Starting in: " + ChatColor.GREEN + arena.getArenaTask().getCountdown() + "s");
            } else {
                lines.add("Waiting...");
            }
        } else if (arena.getGameState() == GameState.PLAYING) {
            // No Map or Date to save space for 8 teams + stats (max 15 lines)
            for (Team team : arena.getTeams()) {
                String status;
                boolean playerInTeam = team.getMembers().contains(player.getUniqueId());
                
                if (team.isBedBroken()) {
                    long aliveCount = team.getMembers().stream()
                            .map(Bukkit::getPlayer)
                            .filter(p -> p != null && p.getGameMode() != org.bukkit.GameMode.SPECTATOR)
                            .count();
                    status = aliveCount > 0 ? ChatColor.GREEN + String.valueOf(aliveCount) : ChatColor.RED + "✘";
                } else {
                    boolean hasMembers = team.getMembers().stream().anyMatch(uuid -> Bukkit.getPlayer(uuid) != null);
                    status = hasMembers ? ChatColor.GREEN + "✔" : ChatColor.RED + "✘";
                }
                
                lines.add(team.getColor().getChatColor() + team.getName().substring(0, 1) + ChatColor.WHITE + " " + team.getName() + ": " + status + (playerInTeam ? ChatColor.GRAY + " YOU" : ""));
            }
            lines.add("");
            lines.add("Kills: " + ChatColor.GREEN + arena.getKills().getOrDefault(player.getUniqueId(), 0));
            lines.add("Final Kills: " + ChatColor.GREEN + arena.getFinalKills().getOrDefault(player.getUniqueId(), 0));
            lines.add("Beds Broken: " + ChatColor.GREEN + arena.getBedBreaks().getOrDefault(player.getUniqueId(), 0));
            lines.add("");
            lines.add("Diamond Tier: " + ChatColor.AQUA + "I"); // Default, could be dynamic
        }

        lines.add("");
        lines.add(ChatColor.YELLOW + "www.spygaming.com");

        // Only update if lines have changed
        List<String> last = lastLines.get(player.getUniqueId());
        if (last != null && last.equals(lines)) {
            return;
        }
        lastLines.put(player.getUniqueId(), new ArrayList<>(lines));

        // Set scoreboard to player if not already set
        if (player.getScoreboard() != scoreboard) {
            player.setScoreboard(scoreboard);
        }

        // Clear old scores (efficiently)
        scoreboard.getEntries().forEach(scoreboard::resetScores);

        // Set new scores
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.isEmpty()) {
                line = " ".repeat(i); // Unique empty space for each empty line
            }
            objective.getScore(line).setScore(lines.size() - i);
        }
    }

    public void removeScoreboard(Player player) {
        scoreboards.remove(player.getUniqueId());
        lastLines.remove(player.getUniqueId());
        player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
    }

    public void clearAll() {
        scoreboards.clear();
        lastLines.clear();
    }
}
