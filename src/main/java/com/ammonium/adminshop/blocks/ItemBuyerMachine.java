package com.ammonium.adminshop.blocks;

import com.ammonium.adminshop.recipes.ShopBuyItemRecipe;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;

import java.util.Optional;

public interface ItemBuyerMachine extends Container, ShopMachine {
    void setRecipe(ResourceLocation recipeId);
    Optional<ShopBuyItemRecipe> getRecipe(ServerLevel level);
}
