package com.ammonium.adminshop.client.gui;

import com.ammonium.adminshop.AdminShop;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public class ProgressBar extends AbstractWidget {
    public static final int MAX_PROGRESS = 8;
    private static final int WIDTH = 22;
    private static final int HEIGHT = 16;
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(AdminShop.MODID, "textures/gui/progress_right.png");

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
    public void updateNarration(NarrationElementOutput narrationElementOutput) {
        return;
    }

    @Override
    public void render(PoseStack poseStack, int pMouseX, int pMouseY, float pPartialTick) {
        super.render(poseStack, pMouseX, pMouseY, pPartialTick);
    }

    @Override
    public void renderToolTip(PoseStack poseStack, int pMouseX, int pMouseY) {
        super.renderToolTip(poseStack, pMouseX, pMouseY);
    }

    @Override
    protected void renderBg(PoseStack poseStack, Minecraft minecraft, int pMouseX, int pMouseY) {
        super.renderBg(poseStack, minecraft, pMouseX, pMouseY);
    }

    @Override
    public void renderButton(@NotNull PoseStack matrix, int mouseX, int mouseY, float partialTicks) {
        //super.renderButton(matrix, x, y, partialTicks);
        AdminShop.LOGGER.debug("Rendering Progress Bar: {}", progress);
        if(!visible) return;

        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, TEXTURE);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        matrix.pushPose();
        // Render Progress Bar
        blit(matrix, x, y, 0, 0, width, height, width, height * 2);
        int progressWidth = (progress >= MAX_PROGRESS) ? width : (int) (width * progress / MAX_PROGRESS);
        if (progressWidth > 0) {
            blit(matrix, x, y, 0, height, progressWidth, height, width, height * 2);
        }

        matrix.popPose();
    }
}
