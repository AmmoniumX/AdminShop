package com.ammonium.adminshop.recipes;

import com.ammonium.adminshop.AdminShop;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModRecipeTypes {
    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES = DeferredRegister.create(
            BuiltInRegistries.RECIPE_TYPE, AdminShop.MODID);

    public static <T extends Recipe<?>> RecipeType<T> registerRecipeType(final String id) {
        return new RecipeType<>() {
            public String toString() {
                return AdminShop.MODID + ":" + id;
            }
        };
    }

    public static final DeferredHolder<RecipeType<?>, RecipeType<BuyItemRecipe>> SHOP_BUY_ITEM = RECIPE_TYPES.register("item_buying", () -> registerRecipeType("item_buying"));

    public static final DeferredHolder<RecipeType<?>, RecipeType<SellItemRecipe>> SHOP_SELL_ITEM = RECIPE_TYPES.register("item_selling", () -> registerRecipeType("item_selling"));

    public static final DeferredHolder<RecipeType<?>, RecipeType<BuyFluidRecipe>> SHOP_BUY_FLUID = RECIPE_TYPES.register("fluid_buying", () -> registerRecipeType("fluid_buying"));

    public static final DeferredHolder<RecipeType<?>, RecipeType<SellFluidRecipe>> SHOP_SELL_FLUID = RECIPE_TYPES.register("fluid_selling", () -> registerRecipeType("fluid_selling"));

    public static void register(IEventBus eventBus) {
        RECIPE_TYPES.register(eventBus);
    }
}
