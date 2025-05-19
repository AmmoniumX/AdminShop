package com.ammonium.adminshop.blocks.interfaces;

import com.ammonium.adminshop.recipes.BuyItemRecipe;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.level.Level;

import java.util.Optional;

public interface ItemBuyerMachine extends Container, ShopMachine {
    void setRecipe(ResourceLocation recipeId);
    Optional<BuyItemRecipe> getRecipe(Level level);
    int getProgress();
}
