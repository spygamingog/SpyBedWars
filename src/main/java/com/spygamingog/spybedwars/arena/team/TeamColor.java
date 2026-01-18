package com.spygamingog.spybedwars.arena.team;

import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.DyeColor;

public enum TeamColor {
    RED("Red", ChatColor.RED, Color.RED, DyeColor.RED),
    BLUE("Blue", ChatColor.BLUE, Color.BLUE, DyeColor.BLUE),
    GREEN("Green", ChatColor.GREEN, Color.GREEN, DyeColor.GREEN),
    YELLOW("Yellow", ChatColor.YELLOW, Color.YELLOW, DyeColor.YELLOW),
    AQUA("Aqua", ChatColor.AQUA, Color.AQUA, DyeColor.CYAN),
    WHITE("White", ChatColor.WHITE, Color.WHITE, DyeColor.WHITE),
    PINK("Pink", ChatColor.LIGHT_PURPLE, Color.FUCHSIA, DyeColor.PINK),
    GRAY("Gray", ChatColor.GRAY, Color.GRAY, DyeColor.GRAY);

    private final String name;
    private final ChatColor chatColor;
    private final Color color;
    private final DyeColor dyeColor;

    TeamColor(String name, org.bukkit.ChatColor chatColor, org.bukkit.Color color, org.bukkit.DyeColor dyeColor) {
        this.name = name;
        this.chatColor = chatColor;
        this.color = color;
        this.dyeColor = dyeColor;
    }

    public org.bukkit.Material getMaterial() {
        return getWool();
    }

    public org.bukkit.Material getWool() {
        return switch (this) {
            case RED -> org.bukkit.Material.RED_WOOL;
            case BLUE -> org.bukkit.Material.BLUE_WOOL;
            case GREEN -> org.bukkit.Material.GREEN_WOOL;
            case YELLOW -> org.bukkit.Material.YELLOW_WOOL;
            case AQUA -> org.bukkit.Material.LIGHT_BLUE_WOOL;
            case WHITE -> org.bukkit.Material.WHITE_WOOL;
            case PINK -> org.bukkit.Material.PINK_WOOL;
            case GRAY -> org.bukkit.Material.GRAY_WOOL;
        };
    }

    public org.bukkit.Material getTerracotta() {
        return switch (this) {
            case RED -> org.bukkit.Material.RED_TERRACOTTA;
            case BLUE -> org.bukkit.Material.BLUE_TERRACOTTA;
            case GREEN -> org.bukkit.Material.GREEN_TERRACOTTA;
            case YELLOW -> org.bukkit.Material.YELLOW_TERRACOTTA;
            case AQUA -> org.bukkit.Material.LIGHT_BLUE_TERRACOTTA;
            case WHITE -> org.bukkit.Material.WHITE_TERRACOTTA;
            case PINK -> org.bukkit.Material.PINK_TERRACOTTA;
            case GRAY -> org.bukkit.Material.GRAY_TERRACOTTA;
        };
    }

    public org.bukkit.Material getGlass() {
        return switch (this) {
            case RED -> org.bukkit.Material.RED_STAINED_GLASS;
            case BLUE -> org.bukkit.Material.BLUE_STAINED_GLASS;
            case GREEN -> org.bukkit.Material.GREEN_STAINED_GLASS;
            case YELLOW -> org.bukkit.Material.YELLOW_STAINED_GLASS;
            case AQUA -> org.bukkit.Material.LIGHT_BLUE_STAINED_GLASS;
            case WHITE -> org.bukkit.Material.WHITE_STAINED_GLASS;
            case PINK -> org.bukkit.Material.PINK_STAINED_GLASS;
            case GRAY -> org.bukkit.Material.GRAY_STAINED_GLASS;
        };
    }

    public org.bukkit.Material getBed() {
        return switch (this) {
            case RED -> org.bukkit.Material.RED_BED;
            case BLUE -> org.bukkit.Material.BLUE_BED;
            case GREEN -> org.bukkit.Material.GREEN_BED;
            case YELLOW -> org.bukkit.Material.YELLOW_BED;
            case AQUA -> org.bukkit.Material.LIGHT_BLUE_BED;
            case WHITE -> org.bukkit.Material.WHITE_BED;
            case PINK -> org.bukkit.Material.PINK_BED;
            case GRAY -> org.bukkit.Material.GRAY_BED;
        };
    }

    public String getName() {
        return name;
    }

    public ChatColor getChatColor() {
        return chatColor;
    }

    public Color getColor() {
        return color;
    }

    public DyeColor getDyeColor() {
        return dyeColor;
    }
}
