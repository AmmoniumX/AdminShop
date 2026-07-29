package com.ammonium.adminshop.blocks.interfaces;

import com.ammonium.adminshop.recipes.BuyFluidRecipe;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import java.util.Optional;

public interface FluidBuyerMachine extends ShopMachine {
    void setRecipe(ResourceLocation recipeId);
    void forceSetRecipe(ResourceLocation recipeId);
    Optional<BuyFluidRecipe> getRecipe(Level level);
    int getProgress();
}
