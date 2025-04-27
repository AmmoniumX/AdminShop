package com.ammonium.adminshop.recipes.interfaces;

import net.minecraft.world.item.ItemStack;

public interface ItemRecipe extends ShopRecipe {
    ItemStack getItem();
}
