package com.spygamingog.spybedwars.shop;

import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

@Getter
public class ShopItem {

    private final String name;
    private final ItemStack itemStack;
    private final Material currency;
    private final int price;
    private final ShopCategory category;

    public ShopItem(String name, ItemStack itemStack, Material currency, int price, ShopCategory category) {
        this.name = name;
        this.itemStack = itemStack;
        this.currency = currency;
        this.price = price;
        this.category = category;
    }
}
