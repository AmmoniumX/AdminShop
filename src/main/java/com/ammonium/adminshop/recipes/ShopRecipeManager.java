package com.ammonium.adminshop.recipes;

import com.ammonium.adminshop.AdminShop;
import com.ammonium.adminshop.blocks.*;
import com.ammonium.adminshop.money.BankAccount;
import com.ammonium.adminshop.money.MoneyManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.crafting.Recipe;

import java.util.Optional;

public class ShopRecipeManager {

    private static BankAccount getAccount(ServerLevel level, ShopMachine machine) {
        return MoneyManager.get(level).getBankAccount(machine.getAccountId());
    }

    public static Optional<ShopBuyItemRecipe> getShopBuyItemRecipe(ServerLevel level, ResourceLocation id) {
        Optional<? extends Recipe<?>> recipe = level.getRecipeManager().byKey(id);
        if (recipe.isEmpty() || !(recipe.get() instanceof ShopBuyItemRecipe)) {
            AdminShop.LOGGER.debug("ShopRecipeManager.getShopBuyItemRecipe: recipe is empty or not a ShopBuyItemRecipe: {}", id);
            return Optional.empty();
        }
        return Optional.of((ShopBuyItemRecipe) recipe.get());
    }

    public static boolean checkForBuyItemRecipe(ServerLevel level, ItemBuyerMachine machine, ShopBuyItemRecipe recipe) {
        return recipe != null && recipe.matches(getAccount(level, machine), machine);
    }

    public static Optional<ShopSellItemRecipe> checkForSellItemRecipe(ServerLevel level, ItemSellerMachine machine) {
        BankAccount account = getAccount(level, machine);
        return level.getRecipeManager().getAllRecipesFor(ModRecipeTypes.SHOP_SELL_ITEM.get())
                .stream()
                .filter(recipe -> recipe.matches(account, machine))
                .findFirst();
    }

    public static Optional<ShopBuyFluidRecipe> getShopBuyFluidRecipe(ServerLevel level, ResourceLocation id) {
        return level.getRecipeManager().getAllRecipesFor(ModRecipeTypes.SHOP_BUY_FLUID.get())
                .stream()
                .filter(recipe -> recipe.getId().equals(id))
                .findFirst();
    }

    public static boolean checkForBuyFluidRecipe(ServerLevel level, FluidBuyerMachine machine, ShopBuyFluidRecipe recipe) {
        return recipe != null && recipe.matches(getAccount(level, machine), machine);
    }

    public static Optional<ShopSellFluidRecipe> checkForSellFluidRecipe(ServerLevel level, FluidSellerMachine machine) {
        BankAccount account = getAccount(level, machine);
        return level.getRecipeManager().getAllRecipesFor(ModRecipeTypes.SHOP_SELL_FLUID.get())
                .stream()
                .filter(recipe -> recipe.matches(account, machine))
                .findFirst();
    }

}
