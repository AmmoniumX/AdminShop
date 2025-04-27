package com.ammonium.adminshop.recipes.interfaces;

import net.minecraft.world.Container;
import net.minecraft.world.item.crafting.Recipe;

public interface ShopRecipe extends Recipe<Container> {
    String getPermit();
    long getPrice();
    String getName();
}
