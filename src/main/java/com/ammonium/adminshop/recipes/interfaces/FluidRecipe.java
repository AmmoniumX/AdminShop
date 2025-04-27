package com.ammonium.adminshop.recipes.interfaces;

import net.minecraftforge.fluids.FluidStack;

public interface FluidRecipe extends ShopRecipe {
    FluidStack getFluid();
}
