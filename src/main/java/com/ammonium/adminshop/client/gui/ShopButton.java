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
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * Shop Item as a clickable button
 */
public class ShopButton extends Button {

    private final ShopRecipe recipe;
    private final ItemRenderer itemRenderer;
    private TextureAtlasSprite fluidTexture;
    private float fluidColorR, fluidColorG, fluidColorB, fluidColorA;
    public boolean isMouseOn = false;

    public ShopButton(ShopRecipe recipe, int x, int y, ItemRenderer renderer, OnPress listener) {
        super(x, y, 16, 16, Component.literal(" "), listener);
        this.itemRenderer = renderer;
        this.recipe = recipe;
        if(recipe instanceof FluidRecipe fluidRecipe) {
            Function<ResourceLocation, TextureAtlasSprite> spriteAtlas = Minecraft.getInstance()
                    .getTextureAtlas(InventoryMenu.BLOCK_ATLAS);
            IClientFluidTypeExtensions properties = IClientFluidTypeExtensions.of(fluidRecipe.getFluid().getFluid());
            ResourceLocation resource = properties.getStillTexture();
            fluidTexture = spriteAtlas.apply(resource);
            int fcol = properties.getTintColor();
            fluidColorR = ((fcol >> 16) & 0xFF) / 255.0F;
            fluidColorG = ((fcol >> 8) & 0xFF) / 255.0F;
            fluidColorB = (fcol & 0xFF) / 255.0F;
            fluidColorA = ((fcol >> 24) & 0xFF) / 255.0F;
        }
    }

    @Override
    public void renderButton(@NotNull PoseStack matrix, int mouseX, int mouseY, float partialTicks) {
        //super.renderButton(matrix, x, y, partialTicks);
        if(!visible)
            return;
        matrix.pushPose();

        //Draw item or fluid
        if(recipe instanceof ItemRecipe itemRecipe) {
            itemRenderer.renderGuiItem(itemRecipe.getDisplayItem(), x, y);
        } else { // Render Fluid
            // Set render for fluid
//            enableScissor(x, y, x + width, y + height);
            RenderSystem.enableBlend();
            RenderSystem.setShader(GameRenderer::getPositionTexShader);
            RenderSystem.setShaderTexture(0, fluidTexture.atlas().location());
            RenderSystem.setShaderColor(fluidColorR, fluidColorG, fluidColorB, fluidColorA);
            blit(matrix, x, y,0, 16, 16, fluidTexture);
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
            RenderSystem.disableBlend();
//            RenderSystem.disableScissor();
        }

        //Highlight background and write item name if hovered or focused
        if(isHoveredOrFocused()){
            isMouseOn = true;
            fill(matrix, x, y, x+width, y+height, 0xFFFFFFDF);
        }else{
            isMouseOn = false;
        }
        //Write quantity based on buttons pressed (sneak & run)
        matrix.pushPose();
//        matrix.translate(0, 0, itemRenderer.blitOffset+201);
        matrix.translate(0, 0, itemRenderer.blitOffset+101);
        matrix.scale(.5f, .5f, 1);
        Font font = Minecraft.getInstance().font;
        int numItems = getNumItems();
        drawString(matrix, font, numItems+"", 2*(x+16)- font.width(numItems+""), 2*(y)+24, 0xFFFFFF);
        if( recipe instanceof ItemRecipe itemRecipe) {
            if (itemRecipe instanceof SellItemRecipe sellRecipe && sellRecipe.getSellType() == SellItemRecipe.SellTypes.TAG) {
                drawString(matrix, font, "#", 2 * x + width * 2 - font.width("#") - 1, 2 * y + 1, 0xFFC921);
            }
            if (itemRecipe.getItem().isPresent() && itemRecipe.getItem().get().hasTag()) {
                drawString(matrix, font, "+NBT", 2 * x + width * 2 - font.width("+NBT") - 1, 2 * y + 1, 0xFF55FF);
            }
        }
        matrix.popPose();

        matrix.popPose();
    }

    private int getNumItems() {
        return recipe.getCount() * getQuantity();
    }

    public int getQuantity(){
        if (recipe instanceof ItemRecipe itemRecipe) {

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
            if (Screen.hasControlDown()) {
                return maxFits;
            }
            if (Screen.hasShiftDown()) {
                return Math.max(maxFits / 2, 1);
            }
            return 1;
        } else if (recipe instanceof FluidRecipe) {
            return 1;
        } else {
            AdminShop.LOGGER.error("ShopButton: Unknown recipe type: {}", recipe.getClass());
            return 0;
        }
    }

    public List<Component> getTooltipContent(){
        int quantity = getQuantity();
        int numItems = getNumItems();
        long price = recipe.getPrice() * quantity;
        List<Component> tooltip = new ArrayList<>();
        String priceFormatted = Screen.hasAltDown() ? MoneyFormat.forcedFormat(price, MoneyFormat.FormatType.RAW) :
                MoneyFormat.forcedFormat(price, MoneyFormat.FormatType.SHORT);
        String description = priceFormatted+
                " "+ numItems +((recipe instanceof ItemRecipe) ? "x " : "mb ")+ recipe.getName();
        tooltip.add(Component.literal(description));
        if (!Objects.equals(recipe.getPermit(), "0") && !recipe.getPermit().isEmpty()) {
            tooltip.add(Component.literal("Requires Permit Tier: "+ recipe.getPermit()));
        }
        return tooltip;
    }

    public ShopRecipe getRecipe(){
        return recipe;
    }
}
