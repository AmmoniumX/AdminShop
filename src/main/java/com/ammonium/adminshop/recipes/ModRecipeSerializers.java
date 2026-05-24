package com.ammonium.adminshop.recipes;

import com.ammonium.adminshop.AdminShop;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModRecipeSerializers {
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS = DeferredRegister.create(Registries.RECIPE_SERIALIZER, AdminShop.MODID);

    public static <T extends Recipe<?>> RecipeType<T> registerRecipeType(final String id) {
        return new RecipeType<>() {
            public String toString() {
                return AdminShop.MODID + ":" + id;
            }
        };
    }

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<BuyItemRecipe>> SHOP_BUY_ITEM_SERIALIZER = RECIPE_SERIALIZERS.register("item_buying", BuyItemRecipe.Serializer::new);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<SellItemRecipe>> SHOP_SELL_ITEM_SERIALIZER = RECIPE_SERIALIZERS.register("item_selling", SellItemRecipe.Serializer::new);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<BuyFluidRecipe>> SHOP_BUY_FLUID_SERIALIZER = RECIPE_SERIALIZERS.register("fluid_buying", BuyFluidRecipe.Serializer::new);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<SellFluidRecipe>> SHOP_SELL_FLUID_SERIALIZER = RECIPE_SERIALIZERS.register("fluid_selling", SellFluidRecipe.Serializer::new);

    public static void register(IEventBus eventBus) {
        AdminShop.LOGGER.info("[AdminShop] ModRecipeSerializers.register() called");
        RECIPE_SERIALIZERS.register(eventBus);
    }

    public static void logRegisteredSerializers() {
        AdminShop.LOGGER.info("[AdminShop] Checking RECIPE_SERIALIZER registry for adminshop entries:");
        BuiltInRegistries.RECIPE_SERIALIZER.keySet().stream()
            .filter(key -> key.getNamespace().equals(AdminShop.MODID))
            .forEach(key -> AdminShop.LOGGER.info("[AdminShop]   registered serializer: {}", key));
    }
}
