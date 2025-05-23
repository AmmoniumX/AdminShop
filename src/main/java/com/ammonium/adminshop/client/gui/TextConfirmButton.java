package com.ammonium.adminshop.client.gui;

import com.ammonium.adminshop.AdminShop;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

/**
 * To swap between Buy and Sell modes
 */
public class TextConfirmButton extends Button {

    private final ResourceLocation GUI = new ResourceLocation(AdminShop.MODID, "textures/gui/detector.png");
    private boolean valid;

    public TextConfirmButton(int x, int y, OnPress listener) {
        super(x, y, 50, 12, Component.translatable("button.adminshop.confirm"), listener);
        this.valid = false;
    }

    public void setValid(boolean newval) {
        this.valid = newval;
    }

    @Override
    public void render(@NotNull PoseStack matrix, int mouseX, int mouseY, float partialTicks) {
        if(!visible) {
            return;
        }
        RenderSystem.setShaderTexture(0, GUI);
        if(valid){
            blit(matrix, x, y,180, 0, 12, 12);
        }else{
            blit(matrix, x, y, 192, 0, 12, 12);
        }
    }
}
