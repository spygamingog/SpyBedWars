package com.spygamingog.spybedwars.listeners;

import com.spygamingog.spybedwars.SpyBedWars;
import com.spygamingog.spybedwars.utils.LocationUtil;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class GlobalListener implements Listener {

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        // Load Quick Buy
        SpyBedWars.getInstance().getQuickBuyManager().loadPlayer(player);
        
        // Delay join logic to prevent lag during server startup/join
        org.bukkit.Bukkit.getScheduler().runTaskLater(SpyBedWars.getInstance(), () -> {
            if (!player.isOnline()) return;

            // Refresh NPCs for player
            SpyBedWars.getInstance().getNpcManager().onPlayerJoin(player);
            
            boolean onJoin = SpyBedWars.getInstance().getConfig().getBoolean("lobby-on-join", true);
            if (onJoin) {
                String lobbyStr = SpyBedWars.getInstance().getConfig().getString("main-lobby");
                if (lobbyStr != null && !lobbyStr.isEmpty()) {
                    Location lobby = LocationUtil.deserialize(lobbyStr);
                    if (lobby != null) {
                        player.teleport(lobby);
                        player.setGameMode(org.bukkit.GameMode.ADVENTURE);
                        SpyBedWars.getInstance().getLobbyItemManager().giveLobbyItems(player);
                    }
                }
            }
            
            checkFly(player);
        }, 5L); // 5 ticks delay
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        SpyBedWars.getInstance().getQuickBuyManager().savePlayer(event.getPlayer());
        SpyBedWars.getInstance().getQuickBuyManager().unloadPlayer(event.getPlayer());
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        checkFly(event.getPlayer());
    }

    private void checkFly(Player player) {
        if (player.getGameMode() == org.bukkit.GameMode.CREATIVE || player.getGameMode() == org.bukkit.GameMode.SPECTATOR) {
            player.setAllowFlight(true);
            return;
        }

        String lobbyStr = SpyBedWars.getInstance().getConfig().getString("main-lobby");
        if (lobbyStr == null || lobbyStr.isEmpty()) {
            player.setAllowFlight(false);
            player.setFlying(false);
            return;
        }

        Location lobby = LocationUtil.deserialize(lobbyStr);
        if (lobby == null || lobby.getWorld() == null) {
            player.setAllowFlight(false);
            player.setFlying(false);
            return;
        }

        if (player.getWorld().equals(lobby.getWorld())) {
            player.setAllowFlight(true);
            player.setFlying(true);
        } else {
            // Only disable if they are not in an arena (Arena handles its own fly state)
            if (!SpyBedWars.getInstance().getArenaManager().getArenaByPlayer(player).isPresent()) {
                player.setAllowFlight(false);
                player.setFlying(false);
            }
        }
    }
}
