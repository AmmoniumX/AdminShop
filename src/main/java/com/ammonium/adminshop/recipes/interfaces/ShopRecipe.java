package com.ammonium.adminshop.recipes.interfaces;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;

public interface ShopRecipe extends Recipe<RecipeInput> {
    String getPermit();
    String getPermitTranslationKey();
    long getPrice();
    int getCount();
    Component getName();
    String getSearchTerm();
}
