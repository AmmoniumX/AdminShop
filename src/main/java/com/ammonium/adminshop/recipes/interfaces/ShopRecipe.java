package com.ammonium.adminshop.recipes.interfaces;

import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.item.crafting.Recipe;

public interface ShopRecipe extends Recipe<Container> {
    String getPermit();
    String getPermitTranslationKey();
    long getPrice();
    int getCount();
    Component getName();
}
