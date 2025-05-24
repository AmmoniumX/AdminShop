package com.ammonium.adminshop.client.jei;

import com.ammonium.adminshop.AdminShop;
import com.ammonium.adminshop.blocks.ModBlocks;
import com.ammonium.adminshop.item.ModItems;
import com.ammonium.adminshop.recipes.RecipeManager;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

@JeiPlugin
public class AdminShopJEI implements IModPlugin {
    @Override
    public ResourceLocation getPluginUid() {
        return new ResourceLocation(AdminShop.MODID, "jei_plugin");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        IGuiHelper guiHelper = registration.getJeiHelpers().getGuiHelper();
        registration.addRecipeCategories(new ShopBuyCategory(guiHelper));
        registration.addRecipeCategories(new ShopSellCategory(guiHelper));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(ShopBuyCategory.SHOP_RECIPE_TYPE, RecipeManager.getAllBuyRecipes(Minecraft.getInstance().level));
        registration.addRecipes(ShopSellCategory.SHOP_RECIPE_TYPE, RecipeManager.getAllSellRecipes(Minecraft.getInstance().level));
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(
                ModBlocks.SHOP.get().asItem().getDefaultInstance(),
                ShopBuyCategory.SHOP_RECIPE_TYPE
        );
        registration.addRecipeCatalyst(
                ModItems.TABLET.get().getDefaultInstance(),
                ShopBuyCategory.SHOP_RECIPE_TYPE
        );
        registration.addRecipeCatalyst(
                ModBlocks.BUYER_1.get().asItem().getDefaultInstance(),
                ShopBuyCategory.SHOP_RECIPE_TYPE
        );
        registration.addRecipeCatalyst(
                ModBlocks.BUYER_2.get().asItem().getDefaultInstance(),
                ShopBuyCategory.SHOP_RECIPE_TYPE
        );
        registration.addRecipeCatalyst(
                ModBlocks.BUYER_3.get().asItem().getDefaultInstance(),
                ShopBuyCategory.SHOP_RECIPE_TYPE
        );

        registration.addRecipeCatalyst(
                ModBlocks.SHOP.get().asItem().getDefaultInstance(),
                ShopSellCategory.SHOP_RECIPE_TYPE
        );
        registration.addRecipeCatalyst(
                ModItems.TABLET.get().getDefaultInstance(),
                ShopSellCategory.SHOP_RECIPE_TYPE
        );
        registration.addRecipeCatalyst(
                ModBlocks.SELLER.get().asItem().getDefaultInstance(),
                ShopSellCategory.SHOP_RECIPE_TYPE
        );
    }
}
