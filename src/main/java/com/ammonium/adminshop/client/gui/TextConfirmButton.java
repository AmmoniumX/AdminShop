package com.ammonium.adminshop.client.gui;

import com.ammonium.adminshop.AdminShop;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

/**
 * To swap between Buy and Sell modes
 */
public class TextConfirmButton extends Button {

    private final Identifier GUI = Identifier.fromNamespaceAndPath(AdminShop.MODID, "textures/gui/detector.png");
    private boolean valid;

    public TextConfirmButton(int x, int y, OnPress listener) {
        super(x, y, 50, 12, Component.translatable("button.adminshop.confirm"), listener, DEFAULT_NARRATION);
        this.valid = false;
    }

    public void setValid(boolean newval) {
        this.valid = newval;
    }

    @Override
    public void extractContents(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTicks) {
        if(!visible) {
            return;
        }
        int x = this.getX();
        int y = this.getY();
        if(valid){
            guiGraphics.blit(RenderPipelines.GUI_TEXTURED, GUI, x, y,180, 0, 12, 12, 256, 256);
        }else{
            guiGraphics.blit(RenderPipelines.GUI_TEXTURED, GUI, x, y, 192, 0, 12, 12, 256, 256);
        }
    }
}
