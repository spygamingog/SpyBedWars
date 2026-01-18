package com.spygamingog.spybedwars.utils;

import com.spygamingog.spycore.api.SpyAPI;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

public class LocationUtil {

    public static String serialize(Location loc) {
        if (loc == null) return "";
        
        String worldName = null;
        try {
            // Try getting world directly
            org.bukkit.World world = loc.getWorld();
            if (world != null) {
                worldName = world.getName();
            }
        } catch (IllegalArgumentException e) {
            // This happens in Paper when world is unloaded
            // Try to extract world name from toString() as a fallback
            String locStr = loc.toString();
            if (locStr.contains("world=")) {
                int start = locStr.indexOf("world=") + 6;
                int end = locStr.indexOf(",", start);
                if (end != -1) {
                    worldName = locStr.substring(start, end);
                }
            }
        }

        if (worldName == null || worldName.isEmpty() || worldName.equalsIgnoreCase("null")) {
            return "";
        }

        // If it's a SpyCore world, try to use its alias for shorter storage
        try {
            org.bukkit.World world = org.bukkit.Bukkit.getWorld(worldName);
            if (world != null) {
                String alias = SpyAPI.getWorldManager().getAliasForWorld(world);
                if (alias != null) worldName = alias;
            }
        } catch (NoClassDefFoundError | Exception ignored) {}
        
        return worldName + "," + loc.getX() + "," + loc.getY() + "," + loc.getZ() + "," + loc.getYaw() + "," + loc.getPitch();
    }

    public static Location deserialize(String s) {
        if (s == null || s.isEmpty()) return null;
        String[] parts = s.split(",");
        if (parts.length < 4) return null;
        
        World world = null;
        String worldName = parts[0];
        try {
            world = SpyAPI.getWorld(worldName);
        } catch (NoClassDefFoundError | Exception ignored) {}
        
        if (world == null) {
            world = Bukkit.getWorld(worldName);
        }

        // If still null and it looks like a SpyCore path, try to load it
        if (world == null && (worldName.startsWith("spycore-worlds/") || worldName.contains("/"))) {
            try {
                String path = worldName.replace("spycore-worlds/", "");
                int lastSlash = path.lastIndexOf("/");
                if (lastSlash != -1) {
                    String container = path.substring(0, lastSlash);
                    String name = path.substring(lastSlash + 1);
                    world = SpyAPI.loadWorld(container, name);
                } else {
                    world = SpyAPI.loadWorld(null, path);
                }
            } catch (NoClassDefFoundError | Exception ignored) {}
        }
        
        if (world == null) {
            // Return a location with a null world for now, but we'll try to resolve it later if needed
            // Actually, Bukkit Location requires a world in constructor, but we can pass null
            // However, it's better to return null if we can't find the world at all
            return null;
        }
        
        double x = Double.parseDouble(parts[1]);
        double y = Double.parseDouble(parts[2]);
        double z = Double.parseDouble(parts[3]);
        float yaw = parts.length > 4 ? Float.parseFloat(parts[4]) : 0;
        float pitch = parts.length > 5 ? Float.parseFloat(parts[5]) : 0;
        return new Location(world, x, y, z, yaw, pitch);
    }

    /**
     * Ensures the location's world is currently loaded and the world object is up-to-date.
     * This is useful if a world has been hibernated and woken up again.
     */
    public static Location validate(Location loc) {
        if (loc == null) return null;
        
        String worldName = null;
        org.bukkit.World world = null;
        try {
            world = loc.getWorld();
            if (world != null) {
                worldName = world.getName();
            }
        } catch (IllegalArgumentException e) {
            // World unloaded, try to extract name from toString
            String locStr = loc.toString();
            if (locStr.contains("world=")) {
                int start = locStr.indexOf("world=") + 6;
                int end = locStr.indexOf(",", start);
                if (end != -1) {
                    worldName = locStr.substring(start, end);
                }
            }
        }

        if (worldName != null) {
            org.bukkit.World currentWorld = org.bukkit.Bukkit.getWorld(worldName);
            if (currentWorld == null) {
                // Try to wake it up via SpyCore
                try {
                    currentWorld = com.spygamingog.spycore.api.SpyAPI.getWorld(worldName);
                } catch (NoClassDefFoundError | Exception ignored) {}
            }

            if (currentWorld != null && currentWorld != world) {
                // World object is stale or newly loaded, update it
                loc.setWorld(currentWorld);
                return loc;
            }
        }

        return loc;
    }
}
