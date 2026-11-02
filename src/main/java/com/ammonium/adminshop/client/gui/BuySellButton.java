package com.ammonium.adminshop.client.gui;

import com.ammonium.adminshop.AdminShop;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

/**
 * To swap between Buy and Sell modes
 */
public class BuySellButton extends Button {

    private final Identifier GUI = Identifier.fromNamespaceAndPath(AdminShop.MODID, "textures/gui/shop_gui.png");
    private static final int TYPE_BUY = 0;
    private static final int TYPE_SELL = 1;

    private final String GUI_BUY = "gui.buy";
    private final String GUI_SELL = "gui.sell";

    private boolean isBuy;

    public BuySellButton(int x, int y, Component buyText, Component sellText, boolean isBuy, OnPress listener) {
        super(x, y, 50, 12, isBuy ? buyText : sellText, listener, DEFAULT_NARRATION);
        this.isBuy = isBuy;
    }

    /**
     * Switches buy and sell mode
     * @return Whether in Buy mode. false = sell mode
     */
    public boolean switchBuySell(){
        isBuy = !isBuy;
        return isBuy;
    }

    @Override
    public void extractContents(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTicks) {
        if(!visible) {
            return;
        }
        int x = this.getX();
        int y = this.getY();

        if(isBuy){
            guiGraphics.blit(RenderPipelines.GUI_TEXTURED, GUI, x, y,195, 68, 50, 12, 256, 256);
        }else{
            guiGraphics.blit(RenderPipelines.GUI_TEXTURED, GUI, x, y, 195, 56, 50, 12, 256, 256);
        }

        Font font = Minecraft.getInstance().font;
        guiGraphics.centeredText(font, I18n.get(GUI_BUY), x+12,
                y+6-Minecraft.getInstance().font.lineHeight/2, 0xFFFFFFFF);
        guiGraphics.centeredText(font, I18n.get(GUI_SELL), x+37,
                y+6-Minecraft.getInstance().font.lineHeight/2, 0xFFFFFFFF);
    }
}
