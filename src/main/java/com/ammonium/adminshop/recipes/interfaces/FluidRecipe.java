package com.ammonium.adminshop.recipes.interfaces;

import net.neoforged.neoforge.fluids.FluidStack;

public interface FluidRecipe extends ShopRecipe {
    FluidStack getFluid();
}
