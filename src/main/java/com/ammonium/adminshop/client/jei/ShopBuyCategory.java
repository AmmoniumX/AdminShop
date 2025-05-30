package com.ammonium.adminshop.client.jei;

import com.ammonium.adminshop.AdminShop;
import com.ammonium.adminshop.blocks.ModBlocks;
import com.ammonium.adminshop.money.MoneyFormat;
import com.ammonium.adminshop.recipes.interfaces.BuyRecipe;
import com.ammonium.adminshop.recipes.interfaces.FluidRecipe;
import com.ammonium.adminshop.recipes.interfaces.ItemRecipe;
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
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public class ShopBuyCategory implements IRecipeCategory<BuyRecipe>{
    public static final RecipeType<BuyRecipe> SHOP_RECIPE_TYPE =
            new RecipeType<>(new ResourceLocation(AdminShop.MODID, "jey_buy_recipe"), BuyRecipe.class);
    private final ResourceLocation GUI = new ResourceLocation(AdminShop.MODID, "textures/gui/jei_buy_category.png");
    private final IDrawable background;
    private final IDrawable icon;

    public ShopBuyCategory(IGuiHelper guiHelper) {
        this.background = guiHelper.createDrawable(GUI, 0, 0, 110, 50);
        this.icon = guiHelper.createDrawableItemStack(ModBlocks.BUYER_1.get().asItem().getDefaultInstance());
    }

    @Override
    public @NotNull RecipeType<BuyRecipe> getRecipeType() {
        return SHOP_RECIPE_TYPE;
    }

    @Override
    public @NotNull Component getTitle() {
        return Component.translatable("jei.category.buy.title");
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
    public void draw(BuyRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        IRecipeCategory.super.draw(recipe, recipeSlotsView, guiGraphics, mouseX, mouseY);
        int priceX = 4;
        int priceY = 30;
        int tierX = 4;
        int tierY = 40;

        // Draw the price
        Font font = Minecraft.getInstance().font;
        String priceFormatted = MoneyFormat.format(recipe.getPrice(), MoneyFormat.FormatType.SHORT);
        String priceText = I18n.get("jei.category.buy.price", priceFormatted);
        guiGraphics.drawString(font, priceText, priceX, priceY, 0xFF555555);

        // Draw the required permit
        if (!recipe.getPermit().isEmpty()) {
            String tierText = I18n.get("jei.requires_permit", I18n.get(recipe.getPermitTranslationKey()));
            guiGraphics.drawString(font, tierText, tierX, tierY, 0xFF555555);
        }
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, BuyRecipe recipe, IFocusGroup focuses) {
        IRecipeSlotBuilder slotBuilder = builder.addSlot(RecipeIngredientRole.OUTPUT, 67, 5);
        if (recipe instanceof ItemRecipe itemRecipe) {
            slotBuilder.addItemStacks(itemRecipe.getValidItems());
        } else if (recipe instanceof FluidRecipe fluidRecipe) {
            slotBuilder.addFluidStack(fluidRecipe.getFluid().getFluid(), fluidRecipe.getFluid().getAmount());
        } else {
            AdminShop.LOGGER.error("ShopBuyCategory: Unknown recipe type: {}", recipe.getClass());
        }
    }
}