package com.ammonium.adminshop.recipes.interfaces;

import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Optional;

public interface ItemRecipe extends ShopRecipe {
    Optional<ItemStack> getItem();
    ItemStack getDisplayItem();
    List<ItemStack> getValidItems();
}
