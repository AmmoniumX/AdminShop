package com.ammonium.adminshop.client.gui;

import com.ammonium.adminshop.shop.ShopItem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.text.NumberFormat;
import java.util.List;

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
        super(Button.builder(Component.literal(" "), listener)
                .pos(x, y)
                .size(16, 16));
        this.itemRenderer = renderer;
        this.item = item;
    }

    @Override
    public void render(@NotNull PoseStack matrix, int mouseX, int mouseY, float partialTicks) {
        //super.renderButton(matrix, x, y, partialTicks);
        if(!visible)
            return;

        int x = getX();
        int y = getY();
        matrix.pushPose();

        //Draw item
        if(item.isItem()) {
            itemRenderer.renderGuiItem(matrix, item.getItem(), x, y);
        }

        //Highlight background and write item name if hovered or focused
        // TODO why is this only triggered upon click
        if(isHoveredOrFocused()){
            System.out.println("Is hovered or focused");
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
        if(item.isTag()){
            drawString(matrix, font, "#", 2*x+width*2-font.width("#")-1, 2*y+1, 0xFFC921);
        }else if(!item.isItem()){
            drawString(matrix, font, "F", 2*x+1, 2*y+1, 0x6666FF);
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
        long price = item.getPrice();
        NumberFormat numberFormat = NumberFormat.getInstance();
        String formatted = numberFormat.format(price);
        return List.of(
                Component.literal("$"+formatted+" "+item.toString()),
                Component.literal("Requires Permit Tier: "+item.getPermitTier())
        );
    }

    public ShopItem getShopItem(){
        return item;
    }
}
