package com.ammonium.adminshop.client.gui;

import com.ammonium.adminshop.AdminShop;
import com.ammonium.adminshop.money.MoneyFormat;
import com.ammonium.adminshop.recipes.BuyItemRecipe;
import com.ammonium.adminshop.recipes.SellItemRecipe;
import com.ammonium.adminshop.recipes.interfaces.FluidRecipe;
import com.ammonium.adminshop.recipes.interfaces.ItemRecipe;
import com.ammonium.adminshop.recipes.interfaces.ShopRecipe;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix3x2fStack;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Shop Item as a clickable button
 */
public class ShopButton extends Button {

    private final RecipeHolder<ShopRecipe> recipe;
    private TextureAtlasSprite fluidTexture;
    private float fluidColorR, fluidColorG, fluidColorB, fluidColorA;
    public boolean isMouseOn = false;

    public ShopButton(RecipeHolder<ShopRecipe> recipe, int x, int y, OnPress listener) {
        super(x, y, 16, 16, Component.empty(), listener, DEFAULT_NARRATION);
        this.recipe = recipe;
        if(recipe.value() instanceof FluidRecipe fluidRecipe) {
            FluidModel properties = Minecraft.getInstance().getModelManager().getFluidStateModelSet()
                    .get(fluidRecipe.getFluid().getFluid().defaultFluidState());
            fluidTexture = properties.stillMaterial().sprite();
            int fcol = properties.fluidTintSource() != null ? properties.fluidTintSource().colorAsStack(fluidRecipe.getFluid()) : -1;
            if (ARGB.alpha(fcol) == 0) { fcol = ARGB.opaque(fcol); }
            fluidColorR = ((fcol >> 16) & 0xFF) / 255.0F;
            fluidColorG = ((fcol >> 8) & 0xFF) / 255.0F;
            fluidColorB = (fcol & 0xFF) / 255.0F;
            fluidColorA = ((fcol >> 24) & 0xFF) / 255.0F;
        }
    }

    @Override
    public void extractContents(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTicks) {
        //super.renderButton(matrix, x, y, partialTicks);
        if(!visible) { return; }

        int x = getX();
        int y = getY();

        Matrix3x2fStack matrix = guiGraphics.pose();
        matrix.pushMatrix();

        //Draw item or fluid
        if(recipe.value() instanceof ItemRecipe itemRecipe) {
            guiGraphics.item(itemRecipe.getDisplayItem(), x, y);
        } else if (recipe.value() instanceof FluidRecipe) { // Render Fluid
            // Set render for fluid
//            enableScissor(x, y, x + width, y + height);
            guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, fluidTexture, x, y, 16, 16,
                    ARGB.colorFromFloat(fluidColorA, fluidColorR, fluidColorG, fluidColorB));
//            RenderSystem.disableScissor();
        }

        //Highlight background and write item name if hovered or focused
        if(isHovered()){
            isMouseOn = true;
            guiGraphics.fill(x, y, x+width, y+height, 0xFFFFFFDF);
        }else{
            isMouseOn = false;
        }
        //Write quantity based on buttons pressed (sneak & run)
        matrix.pushMatrix();
        matrix.scale(.5f, .5f);
        Font font = Minecraft.getInstance().font;
        int numItems = getNumItems();
        guiGraphics.text(font, numItems+"", 2*(x+16)- font.width(numItems+""), 2*(y)+24, 0xFFFFFFFF);
        if(recipe.value() instanceof ItemRecipe itemRecipe) {
            if (itemRecipe instanceof SellItemRecipe sellRecipe && sellRecipe.getSellType() == SellItemRecipe.SellTypes.TAG) {
                guiGraphics.text(font, "#", 2 * x + width * 2 - font.width("#") - 1, 2 * y + 1, 0xFFFFC921);
            }
            if (itemRecipe.getItem().isPresent() && !itemRecipe.getItem().get().getComponentsPatch().isEmpty()) {
                guiGraphics.text(font, "+NBT", 2 * x + width * 2 - font.width("+NBT") - 1, 2 * y + 1, 0xFFFF55FF);
            }
        }
        matrix.popMatrix();

        matrix.popMatrix();
    }

    // Returns the number of items to be placed based on the recipe type
    private int getNumItems() {
        return recipe.value().getCount() * getQuantity();
    }

    // Returns the quantity of **orders** to be placed based on the recipe type, **not** the quantity of items
    public int getQuantity(){
        if (recipe.value() instanceof ItemRecipe itemRecipe) {

            // Get max fits based on stack size
            int maxFits;
            if (itemRecipe instanceof BuyItemRecipe buyRecipe) {
                maxFits = buyRecipe.getItem().get().getMaxStackSize() / buyRecipe.getCount();
            } else if (itemRecipe instanceof SellItemRecipe sellRecipe) {
                if (sellRecipe.getSellType() == SellItemRecipe.SellTypes.ITEM) {
                    maxFits = sellRecipe.getItem().get().getMaxStackSize() / sellRecipe.getCount();
                } else if (sellRecipe.getSellType() == SellItemRecipe.SellTypes.TAG) {
                    maxFits = sellRecipe.getFirstItem().getMaxStackSize() / sellRecipe.getCount();
                } else {
                    AdminShop.LOGGER.error("ShopButton: Unknown sell item recipe type: {}", sellRecipe.getSellType());
                    return 0;
                }
            } else {
                AdminShop.LOGGER.error("ShopButton: Unknown item recipe type: {}", itemRecipe.getClass());
                return 0;
            }

            // Get quantity based on key presses and max fits
            if (Minecraft.getInstance().hasControlDown()) {
                return maxFits;
            }
            if (Minecraft.getInstance().hasShiftDown()) {
                return Math.max(maxFits / 2, 1);
            }
            return 1;
        } else if (recipe.value() instanceof FluidRecipe) {
            return 1;
        } else {
            AdminShop.LOGGER.error("ShopButton: Unknown recipe type: {}", recipe.value().getClass());
            return 0;
        }
    }

    public List<Component> getTooltipContent(){
        int quantity = getQuantity();
        int numItems = getNumItems();
        long price = recipe.value().getPrice() * quantity;
        List<Component> tooltip = new ArrayList<>();
        String priceFormatted = Minecraft.getInstance().hasAltDown()
                ? MoneyFormat.forcedFormat(price, MoneyFormat.FormatType.RAW)
                : MoneyFormat.forcedFormat(price, MoneyFormat.FormatType.SHORT);
        String quantityString = numItems + ((recipe.value() instanceof ItemRecipe) ? "x" : "mb");
        Component description = Component.translatable(
            "shopbutton.description",
            priceFormatted,
            quantityString,
            recipe.value().getName()
        );
        tooltip.add(description);
        if (!recipe.value().getPermit().equals("0") && !recipe.value().getPermit().isEmpty()) {
            tooltip.add(Component.translatable("shopbutton.permit",
                Component.translatable(recipe.value().getPermitTranslationKey())));
        }
        return tooltip;
    }

    public ShopRecipe getRecipe(){
        return recipe.value();
    }
}
