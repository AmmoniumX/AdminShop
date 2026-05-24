package com.ammonium.adminshop.recipes;

import com.ammonium.adminshop.blocks.interfaces.*;
import com.ammonium.adminshop.money.MoneyHelper;
import com.ammonium.adminshop.recipes.interfaces.BuyRecipe;
import com.ammonium.adminshop.recipes.interfaces.SellRecipe;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public class RecipeManager {

    private static MoneyHelper.MoneyAccount getAccount(ServerLevel level, ShopMachine machine) {
        return MoneyHelper.get(level).getAccountById(machine.getTeamId());
    }

    public static List<RecipeHolder<BuyItemRecipe>> getAllBuyItemRecipes(Level level) {
        return level.getRecipeManager().getAllRecipesFor(ModRecipeTypes.SHOP_BUY_ITEM.get());
    }

    public static List<RecipeHolder<BuyFluidRecipe>> getAllBuyFluidRecipes(Level level) {
        return level.getRecipeManager().getAllRecipesFor(ModRecipeTypes.SHOP_BUY_FLUID.get());
    }

    public static Stream<RecipeHolder<BuyRecipe>> getAllBuyRecipes(Level level) {
        return Stream.concat(
                level.getRecipeManager().getAllRecipesFor(ModRecipeTypes.SHOP_BUY_ITEM.get()).stream().map(hol -> new RecipeHolder<BuyRecipe>(hol.id(), hol.value())),
                level.getRecipeManager().getAllRecipesFor(ModRecipeTypes.SHOP_BUY_FLUID.get()).stream().map(hol -> new RecipeHolder<BuyRecipe>(hol.id(), hol.value()))
        );
    }

    public static List<RecipeHolder<SellItemRecipe>> getAllSellItemRecipes(Level level) {
        return level.getRecipeManager().getAllRecipesFor(ModRecipeTypes.SHOP_SELL_ITEM.get());
    }

    public static List<RecipeHolder<SellFluidRecipe>> getAllSellFluidRecipes(Level level) {
        return level.getRecipeManager().getAllRecipesFor(ModRecipeTypes.SHOP_SELL_FLUID.get());
    }

    public static Stream<RecipeHolder<SellRecipe>> getAllSellRecipes(Level level) {
        return Stream.concat(
                level.getRecipeManager().getAllRecipesFor(ModRecipeTypes.SHOP_SELL_ITEM.get()).stream().map(hol -> new RecipeHolder<SellRecipe>(hol.id(), hol.value())),
                level.getRecipeManager().getAllRecipesFor(ModRecipeTypes.SHOP_SELL_FLUID.get()).stream().map(hol -> new RecipeHolder<SellRecipe>(hol.id(), hol.value()))
        );
    }

    public static boolean matches(ItemStack item, BuyItemRecipe recipe) {
        if (recipe.getItem().isEmpty()) return false;

        ItemStack recipeItem = recipe.getItem().get();
        if (item.isEmpty() || recipeItem.isEmpty()) return false;

        // 1.21.1 Native way to compare Item + All Data Components
        // This replaces item.getItem() == recipeItem.getItem() AND the tag check
        return ItemStack.isSameItemSameComponents(item, recipeItem);
    }

    public static Optional<RecipeHolder<BuyItemRecipe>> isBuyItemRecipe(Level level, ItemStack item) {
        return level.getRecipeManager()
                .getAllRecipesFor(ModRecipeTypes.SHOP_BUY_ITEM.get())
                .stream()
                .filter(recipe -> matches(item, recipe.value()))
                .findFirst();
    }

    public static Optional<BuyItemRecipe> getShopBuyItemRecipe(Level level, ResourceLocation id) {
        if (id == null) { return Optional.empty(); }
        return level.getRecipeManager().byKey(id)
                .map(RecipeHolder::value)
                .filter(v -> v instanceof BuyItemRecipe)
                .map(v -> (BuyItemRecipe) v);
    }

    public static boolean checkForBuyItemRecipe(ServerLevel level, ItemBuyerMachine machine, BuyItemRecipe recipe) {
        return recipe != null && recipe.matches(getAccount(level, machine), machine);
    }

    public static boolean canPlaceItemInSeller(Level level, ItemStack item) {
        return level.getRecipeManager().getAllRecipesFor(ModRecipeTypes.SHOP_SELL_ITEM.get())
                .stream()
                .anyMatch(recipe -> recipe.value().isMatchingItemNoCount(item));
    }

    public static Optional<RecipeHolder<SellItemRecipe>> isSellItemRecipe(Level level, ItemStack item) {
        return level.getRecipeManager().getAllRecipesFor(ModRecipeTypes.SHOP_SELL_ITEM.get())
                .stream()
                .filter(recipe -> recipe.value().isMatchingItemStack(item))
                .findFirst();
    }

    public static Optional<RecipeHolder<SellItemRecipe>> checkForSellItemRecipe(ServerLevel level, ItemSellerMachine machine) {
        MoneyHelper.MoneyAccount account = getAccount(level, machine);
        if (account == null) { return Optional.empty(); }
        return level.getRecipeManager().getAllRecipesFor(ModRecipeTypes.SHOP_SELL_ITEM.get())
                .stream()
                .filter(recipe -> recipe.value().matches(account, machine))
                .findFirst();
    }

    public static boolean matches(FluidStack fluid, FluidStack recipeFluid) {
        if (fluid.isEmpty() || recipeFluid.isEmpty()) return false;

        // 1.21.1: Compares both the Fluid type and all data Components
        return FluidStack.isSameFluidSameComponents(fluid, recipeFluid);
    }

    public static Optional<RecipeHolder<BuyFluidRecipe>> isBuyFluidRecipe(Level level, FluidStack fluid) {
        return level.getRecipeManager().getAllRecipesFor(ModRecipeTypes.SHOP_BUY_FLUID.get())
                .stream()
                .filter(recipe -> matches(fluid, recipe.value().getFluid()))
                .findFirst();
    }

    public static Optional<RecipeHolder<BuyFluidRecipe>> getShopBuyFluidRecipe(Level level, ResourceLocation id) {
        return level.getRecipeManager().getAllRecipesFor(ModRecipeTypes.SHOP_BUY_FLUID.get())
                .stream()
                .filter(recipe -> recipe.id().equals(id))
                .findFirst();
    }

    public static boolean checkForBuyFluidRecipe(ServerLevel level, FluidBuyerMachine machine, BuyFluidRecipe recipe) {
        return recipe != null && recipe.matches(getAccount(level, machine), machine);
    }

    public static Optional<RecipeHolder<SellFluidRecipe>> isSellFluidRecipe(Level level, FluidStack fluid) {
        return level.getRecipeManager().getAllRecipesFor(ModRecipeTypes.SHOP_SELL_FLUID.get())
                .stream()
                .filter(recipe -> matches(fluid, recipe.value().getFluid()))
                .findFirst();
    }

    public static Optional<RecipeHolder<SellFluidRecipe>> checkForSellFluidRecipe(ServerLevel level, FluidSellerMachine machine) {
        MoneyHelper.MoneyAccount account = getAccount(level, machine);
        if (account == null) { return Optional.empty(); }
        return level.getRecipeManager().getAllRecipesFor(ModRecipeTypes.SHOP_SELL_FLUID.get())
                .stream()
                .filter(recipe -> recipe.value().matches(account, machine))
                .findFirst();
    }

}
