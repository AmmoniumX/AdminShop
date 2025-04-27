package com.ammonium.adminshop.client.jei;

import com.ammonium.adminshop.AdminShop;
import com.ammonium.adminshop.blocks.ModBlocks;
import com.ammonium.adminshop.money.MoneyFormat;
import com.ammonium.adminshop.recipes.SellFluidRecipe;
import com.ammonium.adminshop.recipes.SellItemRecipe;
import com.ammonium.adminshop.recipes.interfaces.SellRecipe;
import com.mojang.blaze3d.vertex.PoseStack;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public class ShopSellCategory implements IRecipeCategory<SellRecipe>{
    public static final RecipeType<SellRecipe> SHOP_RECIPE_TYPE =
            new RecipeType<>(new ResourceLocation(AdminShop.MODID, "jei_sell_recipe"), SellRecipe.class);
    private final ResourceLocation GUI = new ResourceLocation(AdminShop.MODID, "textures/gui/jei_sell_category.png");
    private final IDrawable background;
    private final IDrawable icon;

    public ShopSellCategory(IGuiHelper guiHelper) {
        this.background = guiHelper.createDrawable(GUI, 0, 0, 110, 100);
        this.icon = guiHelper.createDrawableItemStack(ModBlocks.SELLER.get().asItem().getDefaultInstance());
    }

    @Override
    public @NotNull RecipeType<SellRecipe> getRecipeType() {
        return SHOP_RECIPE_TYPE;
    }

    @Override
    public @NotNull Component getTitle() {
        return Component.translatable("jei.category.sell.title");
    }

    @Override
    public @NotNull IDrawable getBackground() {
        return background;
    }

    @Override
    public @NotNull IDrawable getIcon() {
        return icon;
    }

    @Override
    public void draw(SellRecipe recipe, IRecipeSlotsView recipeSlotsView, PoseStack stack, double mouseX, double mouseY) {
        IRecipeCategory.super.draw(recipe, recipeSlotsView, stack, mouseX, mouseY);
        int priceX = 8;
        int priceY = 60;
        int tierX = 8;
        int tierY = 80;

        // Draw the price
        String priceFormatted = MoneyFormat.format(recipe.getPrice(), MoneyFormat.FormatType.SHORT);
        String priceText = "Sell Price: "+ priceFormatted;
        Minecraft.getInstance().font.draw(stack, priceText, priceX, priceY, 0xFF555555);

        // Draw the required tier
        String tierText = "Requires Tier: "+((recipe.getPermit().equals("0") || recipe.getPermit().isEmpty()) ? "None" : recipe.getPermit());
        Minecraft.getInstance().font.draw(stack, tierText, tierX, tierY, 0xFF555555);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, SellRecipe recipe, IFocusGroup focuses) {
        IRecipeSlotBuilder slotBuilder = builder.addSlot(RecipeIngredientRole.INPUT, 24, 5);
        if (recipe instanceof SellItemRecipe itemRecipe) {
            slotBuilder.addItemStack(itemRecipe.getItem());
        } else if (recipe instanceof SellFluidRecipe fluidRecipe) {
            slotBuilder.addFluidStack(fluidRecipe.getFluid().getFluid(), fluidRecipe.getFluid().getAmount());
        } else {
            AdminShop.LOGGER.error("ShopSellCategory: Unknown recipe type: {}", recipe.getClass());
        }
    }
}