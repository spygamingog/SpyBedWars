package com.spygamingog.spybedwars.arena.generator;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

@Getter
@Setter
public class Generator {

    private Location location;
    private final GeneratorType type;
    private int delay;
    private int timer;
    private org.bukkit.entity.ArmorStand hologram;

    public Generator(Location location, GeneratorType type) {
        this.location = location;
        this.type = type;
        this.delay = type.getDefaultDelay();
        this.timer = delay;
    }

    public void spawnHologram() {
        if (location == null || location.getWorld() == null) {
            com.spygamingog.spybedwars.SpyBedWars.getInstance().getLogger().warning("Cannot spawn hologram: location or world is null!");
            return;
        }
        
        // If hologram already exists and is valid, just update it
        if (hologram != null && hologram.isValid()) {
            updateHologram();
            return;
        }
        
        // Use a 1-tick delay to ensure the world is ready
        org.bukkit.Bukkit.getScheduler().runTaskLater(com.spygamingog.spybedwars.SpyBedWars.getInstance(), () -> {
            try {
                if (location == null || location.getWorld() == null) return;

                // Ensure the chunk is loaded before spawning
                org.bukkit.Chunk chunk = location.getChunk();
                if (!chunk.isLoaded()) {
                    chunk.load();
                }

                // Cleanup orphaned armor stands nearby
                location.getWorld().getEntitiesByClass(org.bukkit.entity.ArmorStand.class).stream()
                    .filter(as -> as.getLocation().distanceSquared(location) < 4)
                    .filter(as -> as.hasMetadata("spybedwars_generator") || (as.getCustomName() != null && as.getCustomName().contains("Generator")))
                    .forEach(org.bukkit.entity.Entity::remove);

                // Use the most basic spawn method
                hologram = (org.bukkit.entity.ArmorStand) location.getWorld().spawnEntity(location.clone().add(0, 0.8, 0), org.bukkit.entity.EntityType.ARMOR_STAND);
                
                if (hologram != null) {
                    hologram.setBasePlate(false);
                    hologram.setVisible(false);
                    hologram.setGravity(false);
                    hologram.setCanPickupItems(false);
                    hologram.setCustomNameVisible(true);
                    hologram.setSmall(true);
                    hologram.setMarker(true);
                    hologram.setMetadata("spybedwars_generator", new org.bukkit.metadata.FixedMetadataValue(com.spygamingog.spybedwars.SpyBedWars.getInstance(), true));
                    
                    updateHologram();
                    com.spygamingog.spybedwars.SpyBedWars.getInstance().getLogger().info("Successfully spawned " + type.name() + " hologram at " + location.getBlockX() + ", " + location.getBlockY() + ", " + location.getBlockZ());
                } else {
                    com.spygamingog.spybedwars.SpyBedWars.getInstance().getLogger().severe("Failed to spawn " + type.name() + " hologram: Spawned entity is null!");
                }
            } catch (Exception e) {
                com.spygamingog.spybedwars.SpyBedWars.getInstance().getLogger().severe("Exception while spawning " + type.name() + " hologram: " + e.getMessage());
                e.printStackTrace();
            }
        }, 1L);
    }

    public void updateHologram() {
        if (hologram == null || !hologram.isValid()) return;
        
        String typeName = type.name().substring(0, 1).toUpperCase() + type.name().substring(1).toLowerCase();
        org.bukkit.ChatColor color = switch (type) {
            case IRON -> org.bukkit.ChatColor.WHITE;
            case GOLD -> org.bukkit.ChatColor.GOLD;
            case DIAMOND -> org.bukkit.ChatColor.AQUA;
            case EMERALD -> org.bukkit.ChatColor.GREEN;
        };
        
        hologram.setCustomName(color + "" + org.bukkit.ChatColor.BOLD + typeName + " Generator " + 
                             org.bukkit.ChatColor.YELLOW + "(" + timer + "s)");
    }

    public void remove() {
        if (hologram != null) {
            hologram.remove();
            hologram = null;
        }
    }

    public void tick() {
        if (timer > 0) {
            timer--;
            updateHologram();
        } else {
            spawnItem();
            timer = delay;
            updateHologram();
        }
    }

    private void spawnItem() {
        if (location.getWorld() != null) {
            location.getWorld().dropItemNaturally(location, new ItemStack(type.getMaterial()));
        }
    }
}
