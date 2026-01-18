package com.spygamingog.spybedwars.arena.generator;

import org.bukkit.Material;

public enum GeneratorType {
    IRON(Material.IRON_INGOT, 1),
    GOLD(Material.GOLD_INGOT, 4),
    DIAMOND(Material.DIAMOND, 30),
    EMERALD(Material.EMERALD, 60);

    private final Material material;
    private final int defaultDelay;

    GeneratorType(Material material, int defaultDelay) {
        this.material = material;
        this.defaultDelay = defaultDelay;
    }

    public Material getMaterial() {
        return material;
    }

    public int getDefaultDelay() {
        return defaultDelay;
    }
}
