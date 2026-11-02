package com.ammonium.adminshop.recipes.interfaces;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeBookCategories;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeInput;

public interface ShopRecipe extends Recipe<RecipeInput> {
    String getPermit();
    String getPermitTranslationKey();
    long getPrice();
    int getCount();
    Component getName();
    String getSearchTerm();

    // Shop recipes have no ingredients, so the recipe manager must not try to place them
    @Override
    default boolean isSpecial() {
        return true;
    }

    @Override
    default boolean showNotification() {
        return false;
    }

    @Override
    default String group() {
        return "";
    }

    @Override
    default PlacementInfo placementInfo() {
        return PlacementInfo.NOT_PLACEABLE;
    }

    @Override
    default RecipeBookCategory recipeBookCategory() {
        return RecipeBookCategories.CRAFTING_MISC;
    }
}
