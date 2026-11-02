package com.ammonium.adminshop.client.gui;

import com.ammonium.adminshop.AdminShop;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.joml.Matrix3x2fStack;

public class ProgressBar extends AbstractWidget {
    public static final int MAX_PROGRESS = 8;
    private static final int WIDTH = 22;
    private static final int HEIGHT = 16;
    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath(AdminShop.MODID, "textures/gui/progress_right.png");

    private int progress = 0;

    public ProgressBar(int x, int y) {
        super(x, y, WIDTH, HEIGHT, Component.translatable("gui.adminshop.progress_bar"));
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }

    public int getProgress() {
        return progress;
    }

    @Override
    public void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
        return;
    }

    @Override
    public void extractWidgetRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTicks) {
        //super.renderButton(matrix, x, y, partialTicks);
//        AdminShop.LOGGER.debug("Rendering Progress Bar: {}", progress);
        if(!visible) return;
        int x = this.getX();
        int y = this.getY();

        Matrix3x2fStack poseStack = guiGraphics.pose();
        poseStack.pushMatrix();
        // Render Progress Bar
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, 0, 0, width, height, width, height * 2);
        int progressWidth = (progress >= MAX_PROGRESS) ? width : (int) (width * progress / MAX_PROGRESS);
        if (progressWidth > 0) {
            guiGraphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, 0, height, progressWidth, height, width, height * 2);
        }

        poseStack.popMatrix();
    }
}
