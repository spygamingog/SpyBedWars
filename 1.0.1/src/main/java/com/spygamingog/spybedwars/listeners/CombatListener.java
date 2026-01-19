package com.spygamingog.spybedwars.listeners;

import com.spygamingog.spybedwars.SpyBedWars;
import com.spygamingog.spybedwars.api.arena.GameState;
import com.spygamingog.spybedwars.arena.Arena;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;

import java.util.Optional;

public class CombatListener implements Listener {

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        // Only apply if they're joining an active game (unlikely but safe)
        Player player = event.getPlayer();
        Optional<Arena> arenaOpt = SpyBedWars.getInstance().getArenaManager().getArenaByPlayer(player);
        if (arenaOpt.isPresent() && arenaOpt.get().getGameState() == GameState.PLAYING) {
            set18Combat(player);
        } else {
            resetCombat(player);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRespawn(org.bukkit.event.player.PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        Optional<Arena> arenaOpt = SpyBedWars.getInstance().getArenaManager().getArenaByPlayer(player);
        if (arenaOpt.isPresent() && arenaOpt.get().getGameState() == GameState.PLAYING) {
            set18Combat(player);
        } else {
            resetCombat(player);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player damager)) return;

        Optional<Arena> arenaOpt = SpyBedWars.getInstance().getArenaManager().getArenaByPlayer(damager);
        if (arenaOpt.isPresent() && arenaOpt.get().getGameState() == GameState.PLAYING) {
            // Disable Sweeping damage (1.9+)
            if (event.getCause() == EntityDamageByEntityEvent.DamageCause.ENTITY_SWEEP_ATTACK) {
                event.setCancelled(true);
                return;
            }
            
            // Note: Visual sweep animation is hard to stop without packets/modifying attributes,
            // but we already set attack speed to 16.0 which minimizes it.
        }
    }

    public void set18Combat(Player player) {
        AttributeInstance attribute = player.getAttribute(Attribute.GENERIC_ATTACK_SPEED);
        if (attribute != null) {
            attribute.setBaseValue(16.0); // 1.8 style attack speed
        }
    }

    public void resetCombat(Player player) {
        AttributeInstance attribute = player.getAttribute(Attribute.GENERIC_ATTACK_SPEED);
        if (attribute != null) {
            attribute.setBaseValue(4.0); // Default 1.9+ attack speed
        }
    }

    private boolean isCritical(Player player) {
        return player.getFallDistance() > 0.0F && 
               !player.isOnGround() && 
               !player.isInsideVehicle() && 
               !player.hasPotionEffect(org.bukkit.potion.PotionEffectType.BLINDNESS) && 
               player.getLocation().getBlock().getType() != org.bukkit.Material.LADDER && 
               player.getLocation().getBlock().getType() != org.bukkit.Material.VINE && 
               player.getEyeLocation().getBlock().getType() != org.bukkit.Material.WATER;
    }
}
