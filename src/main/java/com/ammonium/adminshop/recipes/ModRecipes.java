package com.ammonium.adminshop.recipes;

import com.ammonium.adminshop.AdminShop;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModRecipes {
    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES = DeferredRegister.create(ForgeRegistries.RECIPE_TYPES, AdminShop.MODID);
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS = DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, AdminShop.MODID);

    public static <T extends Recipe<?>> RecipeType<T> registerRecipeType(final String id) {
        return new RecipeType<>() {
            public String toString() {
                return AdminShop.MODID + ":" + id;
            }
        };
    }

    public static final RegistryObject<RecipeType<ShopBuyItemRecipe>> SHOP_BUY_ITEM_TYPE = RECIPE_TYPES.register("shop_buy_item", () -> registerRecipeType("shop_buy_item"));
    public static final RegistryObject<RecipeSerializer<ShopBuyItemRecipe>> SHOP_BUY_ITEM_SERIALIZER = RECIPE_SERIALIZERS.register("shop_buy_item", ShopBuyItemRecipe.Serializer::new);

    public static final RegistryObject<RecipeType<ShopSellItemRecipe>> SHOP_SELL_ITEM_TYPE = RECIPE_TYPES.register("shop_sell_item", () -> registerRecipeType("shop_sell_item"));
    public static final RegistryObject<RecipeSerializer<ShopSellItemRecipe>> SHOP_SELL_ITEM_SERIALIZER = RECIPE_SERIALIZERS.register("shop_sell_item", ShopSellItemRecipe.Serializer::new);

    public static final RegistryObject<RecipeType<ShopBuyFluidRecipe>> SHOP_BUY_FLUID_TYPE = RECIPE_TYPES.register("shop_buy_fluid", () -> registerRecipeType("shop_buy_fluid"));
    public static final RegistryObject<RecipeSerializer<ShopBuyFluidRecipe>> SHOP_BUY_FLUID_SERIALIZER = RECIPE_SERIALIZERS.register("shop_buy_fluid", ShopBuyFluidRecipe.Serializer::new);

    public static final RegistryObject<RecipeType<ShopSellFluidRecipe>> SHOP_SELL_FLUID_TYPE = RECIPE_TYPES.register("shop_sell_fluid", () -> registerRecipeType("shop_sell_fluid"));
    public static final RegistryObject<RecipeSerializer<ShopSellFluidRecipe>> SHOP_SELL_FLUID_SERIALIZER = RECIPE_SERIALIZERS.register("shop_sell_fluid", ShopSellFluidRecipe.Serializer::new);

    public static void register(IEventBus eventBus) {
        RECIPE_TYPES.register(eventBus);
        RECIPE_SERIALIZERS.register(eventBus);
    }
}
