package com.ammonium.adminshop.recipes;

import com.ammonium.adminshop.AdminShop;
import com.ammonium.adminshop.blocks.interfaces.*;
import com.ammonium.adminshop.money.BankAccount;
import com.ammonium.adminshop.money.MoneyManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.Level;
import net.minecraftforge.fluids.FluidStack;

import java.util.List;
import java.util.Optional;

public class RecipeManager {

    private static BankAccount getAccount(ServerLevel level, ShopMachine machine) {
        return MoneyManager.get(level).getBankAccount(machine.getAccountId());
    }

    public static List<BuyItemRecipe> getAllBuyItemRecipes(Level level) {
        return level.getRecipeManager().getAllRecipesFor(ModRecipeTypes.SHOP_BUY_ITEM.get());
    }

    public static List<SellItemRecipe> getAllSellItemRecipes(Level level) {
        return level.getRecipeManager().getAllRecipesFor(ModRecipeTypes.SHOP_SELL_ITEM.get());
    }

    public static List<BuyFluidRecipe> getAllBuyFluidRecipes(Level level) {
        return level.getRecipeManager().getAllRecipesFor(ModRecipeTypes.SHOP_BUY_FLUID.get());
    }

    public static List<SellFluidRecipe> getAllSellFluidRecipes(Level level) {
        return level.getRecipeManager().getAllRecipesFor(ModRecipeTypes.SHOP_SELL_FLUID.get());
    }

    public static boolean matches(ItemStack item, ItemStack recipeItem) {
        if (item.isEmpty() || recipeItem.isEmpty()) { return false; }
        if (item.getItem() != recipeItem.getItem()) { return false; }
        if (recipeItem.hasTag()) {
            return item.getTag() != null && item.getTag().equals(recipeItem.getTag());
        } else {
            return true;
        }
    }

    public static Optional<BuyItemRecipe> isBuyItemRecipe(Level level, ItemStack item) {
        List<BuyItemRecipe> candidates = level.getRecipeManager().getAllRecipesFor(ModRecipeTypes.SHOP_BUY_ITEM.get())
                .stream()
                .filter(recipe -> matches(item, recipe.getItem()))
                .toList();
        Optional<BuyItemRecipe> firstWithNBT = candidates
                .stream()
                .filter(recipe -> recipe.getItem().hasTag())
                .findFirst();
        return firstWithNBT.or(() -> candidates.stream().findFirst());
    }

    public static Optional<BuyItemRecipe> getShopBuyItemRecipe(Level level, ResourceLocation id) {
        if (id == null) { return Optional.empty(); }
        Optional<? extends Recipe<?>> recipe = level.getRecipeManager().byKey(id);
        if (recipe.isEmpty() || !(recipe.get() instanceof BuyItemRecipe)) {
            AdminShop.LOGGER.debug("ShopRecipeManager.getShopBuyItemRecipe: recipe is empty or not a ShopBuyItemRecipe: {}", id);
            return Optional.empty();
        }
        return Optional.of((BuyItemRecipe) recipe.get());
    }

    public static boolean checkForBuyItemRecipe(ServerLevel level, ItemBuyerMachine machine, BuyItemRecipe recipe) {
        return recipe != null && recipe.matches(getAccount(level, machine), machine);
    }

    public static Optional<SellItemRecipe> isSellItemRecipe(Level level, ItemStack item) {
        List<SellItemRecipe> candidates = level.getRecipeManager().getAllRecipesFor(ModRecipeTypes.SHOP_SELL_ITEM.get())
                .stream()
                .filter(recipe -> matches(item, recipe.getItem()))
                .toList();
        Optional<SellItemRecipe> firstWithNBT = candidates
                .stream()
                .filter(recipe -> recipe.getItem().hasTag())
                .findFirst();
        return firstWithNBT.or(() -> candidates.stream().findFirst());
    }

    public static Optional<SellItemRecipe> checkForSellItemRecipe(ServerLevel level, ItemSellerMachine machine) {
        BankAccount account = getAccount(level, machine);
        return level.getRecipeManager().getAllRecipesFor(ModRecipeTypes.SHOP_SELL_ITEM.get())
                .stream()
                .filter(recipe -> recipe.matches(account, machine))
                .findFirst();
    }

    public static boolean matches(FluidStack fluid, FluidStack recipeFluid) {
        if (fluid.isEmpty() || recipeFluid.isEmpty()) { return false; }
        return fluid.getFluid() == recipeFluid.getFluid();
    }

    public static Optional<BuyFluidRecipe> isBuyFluidRecipe(Level level, FluidStack fluid) {
        AdminShop.LOGGER.debug("ShopRecipeManager.isBuyFluidRecipe: checking for recipe for fluid {}", fluid);
        return level.getRecipeManager().getAllRecipesFor(ModRecipeTypes.SHOP_BUY_FLUID.get())
                .stream()
                .filter(recipe -> matches(fluid, recipe.getFluid()))
                .findFirst();
    }

    public static Optional<BuyFluidRecipe> getShopBuyFluidRecipe(Level level, ResourceLocation id) {
        return level.getRecipeManager().getAllRecipesFor(ModRecipeTypes.SHOP_BUY_FLUID.get())
                .stream()
                .filter(recipe -> recipe.getId().equals(id))
                .findFirst();
    }

    public static boolean checkForBuyFluidRecipe(ServerLevel level, FluidBuyerMachine machine, BuyFluidRecipe recipe) {
        return recipe != null && recipe.matches(getAccount(level, machine), machine);
    }

    public static Optional<SellFluidRecipe> isSellFluidRecipe(Level level, FluidStack fluid) {
        return level.getRecipeManager().getAllRecipesFor(ModRecipeTypes.SHOP_SELL_FLUID.get())
                .stream()
                .filter(recipe -> matches(fluid, recipe.getFluid()))
                .findFirst();
    }

    public static Optional<SellFluidRecipe> checkForSellFluidRecipe(ServerLevel level, FluidSellerMachine machine) {
        BankAccount account = getAccount(level, machine);
        return level.getRecipeManager().getAllRecipesFor(ModRecipeTypes.SHOP_SELL_FLUID.get())
                .stream()
                .filter(recipe -> recipe.matches(account, machine))
                .findFirst();
    }

}
