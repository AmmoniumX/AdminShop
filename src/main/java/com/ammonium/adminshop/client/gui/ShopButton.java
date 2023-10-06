package com.ammonium.adminshop.client.gui;

import com.ammonium.adminshop.shop.ShopItem;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import org.jetbrains.annotations.NotNull;

import java.text.NumberFormat;
import java.util.List;
import java.util.function.Function;

/**
 * Shop Item as a clickable button
 */
public class ShopButton extends Button {

    private ShopItem item;
    private ItemRenderer itemRenderer;
    private TextureAtlasSprite fluidTexture;
    private float fluidColorR, fluidColorG, fluidColorB, fluidColorA;
    public boolean isMouseOn = false;

    public ShopButton(ShopItem item, int x, int y, ItemRenderer renderer, OnPress listener) {
        super(x, y, 16, 16, Component.literal(" "), listener);
        this.itemRenderer = renderer;
        this.item = item;
        if(!item.isItem()) {
            Function<ResourceLocation, TextureAtlasSprite> spriteAtlas = Minecraft.getInstance()
                    .getTextureAtlas(InventoryMenu.BLOCK_ATLAS);
            IClientFluidTypeExtensions properties = IClientFluidTypeExtensions.of(item.getFluid().getFluid());
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
        if(item.isItem()) {
            itemRenderer.renderGuiItem(item.getItem(), x, y);
        } else { // Render Fluid
            RenderSystem.bindTexture(fluidTexture.atlas().getId());
            RenderSystem.setShaderColor(fluidColorR, fluidColorG, fluidColorB, fluidColorA);
            RenderSystem.setShaderTexture(0,
                    fluidTexture.atlas().location());
            blit(matrix, x, y,0, 16, 16, fluidTexture);
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
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
        matrix.translate(0, 0, 201);
        matrix.scale(.5f, .5f, 1);
        Font font = Minecraft.getInstance().font;
        drawString(matrix, font, getQuantity()+"", 2*(x+16)- font.width(getQuantity()+""), 2*(y)+24, 0xFFFFFF);
        if(item.isTag()) {
            drawString(matrix, font, "#", 2 * x + width * 2 - font.width("#") - 1, 2 * y + 1, 0xFFC921);
        } else if (item.hasNBT()) {
            drawString(matrix, font, "+NBT", 2 * x + width * 2 - font.width("+NBT") - 1, 2 * y + 1, 0xFF55FF);
        }
        matrix.popPose();

        matrix.popPose();
    }

    public int getQuantity(){
        if(Screen.hasControlDown() && Screen.hasShiftDown())
            return item.isItem() ? 64 : 1000;
        else if(Screen.hasControlDown() || Screen.hasShiftDown())
            return item.isItem() ? 16 : 100;
        else
            return 1;
    }

    public List<Component> getTooltipContent(){
        long price = item.getPrice() * getQuantity();
        NumberFormat numberFormat = NumberFormat.getInstance();
        String formatted = numberFormat.format(price);
        return List.of(
                Component.literal("$"+formatted+" "+getQuantity()+((item.isItem()) ? "x " : "mb ")+item.toString()),
                Component.literal("Requires Permit Tier: "+item.getPermitTier())
        );
    }

    public ShopItem getShopItem(){
        return item;
    }
}
