package com.ammonium.adminshop.blocks;

import com.ammonium.adminshop.recipes.ShopBuyFluidRecipe;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;

import java.util.Optional;

public interface FluidBuyerMachine extends ShopMachine {
    void setRecipe(ResourceLocation recipeId);
    Optional<ShopBuyFluidRecipe> getRecipe(ServerLevel level);
}
