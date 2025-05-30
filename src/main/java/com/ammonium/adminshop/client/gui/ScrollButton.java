package com.ammonium.adminshop.client.gui;

import com.ammonium.adminshop.AdminShop;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;


public class ScrollButton extends Button {

    private final ResourceLocation GUI = new ResourceLocation(AdminShop.MODID, "textures/gui/shop_gui.png");
    private boolean isUp;

    public ScrollButton(int x, int y, boolean isUp, OnPress listener){
        super(x, y, 16, 16, Component.empty(), listener, DEFAULT_NARRATION);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        //super.renderButton(matrix, mouseX, mouseY, partialTicks);
        if(!visible) {
            return;
        }

        RenderSystem.setShaderTexture(0, GUI);
        //TODO add button textures. Use background arrows for now
    }
}
