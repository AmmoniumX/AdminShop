package com.ammonium.adminshop.recipes;

import com.ammonium.adminshop.AdminShop;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModRecipeTypes {
    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES = DeferredRegister.create(ForgeRegistries.RECIPE_TYPES, AdminShop.MODID);

    public static <T extends Recipe<?>> RecipeType<T> registerRecipeType(final String id) {
        return new RecipeType<>() {
            public String toString() {
                return AdminShop.MODID + ":" + id;
            }
        };
    }

    public static final RegistryObject<RecipeType<BuyItemRecipe>> SHOP_BUY_ITEM = RECIPE_TYPES.register("shop_buy_item", () -> registerRecipeType("shop_buy_item"));

    public static final RegistryObject<RecipeType<SellItemRecipe>> SHOP_SELL_ITEM = RECIPE_TYPES.register("shop_sell_item", () -> registerRecipeType("shop_sell_item"));

    public static final RegistryObject<RecipeType<BuyFluidRecipe>> SHOP_BUY_FLUID = RECIPE_TYPES.register("shop_buy_fluid", () -> registerRecipeType("shop_buy_fluid"));

    public static final RegistryObject<RecipeType<SellFluidRecipe>> SHOP_SELL_FLUID = RECIPE_TYPES.register("shop_sell_fluid", () -> registerRecipeType("shop_sell_fluid"));

    public static void register(IEventBus eventBus) {
        RECIPE_TYPES.register(eventBus);
    }
}
