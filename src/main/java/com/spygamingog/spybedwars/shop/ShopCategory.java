package com.spygamingog.spybedwars.shop;

import lombok.Getter;
import org.bukkit.Material;

@Getter
public enum ShopCategory {
    BLOCKS("Blocks", Material.WHITE_WOOL, 1),
    MELEE("Melee", Material.GOLDEN_SWORD, 2),
    ARMOR("Armor", Material.CHAINMAIL_BOOTS, 3),
    TOOLS("Tools", Material.STONE_PICKAXE, 4),
    RANGED("Ranged", Material.BOW, 5),
    POTIONS("Potions", Material.BREWING_STAND, 6),
    UTILITY("Utility", Material.TNT, 7),
    QUICK_BUY("Quick Buy", Material.NETHER_STAR, 0);

    private final String displayName;
    private final Material icon;
    private final int slot;

    ShopCategory(String displayName, Material icon, int slot) {
        this.displayName = displayName;
        this.icon = icon;
        this.slot = slot;
    }
}
