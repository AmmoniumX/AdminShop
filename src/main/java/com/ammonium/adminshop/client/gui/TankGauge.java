package com.ammonium.adminshop.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.fluids.IFluidTank;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Function;

public class TankGauge extends AbstractWidget {

    private IFluidTank tank;
    private TextureAtlasSprite fluidTexture;
    private float fluidColorR, fluidColorG, fluidColorB, fluidColorA;
    private int textureWidth, textureHeight;
    public boolean isMouseOn = false;

    private Fluid getFluid() {
        if (tank.getFluid().isEmpty()) {return null;}
        return tank.getFluid().getFluid();
    }

    private void setFluidTextures() {
        if(!tank.getFluid().isEmpty()) {
            Function<ResourceLocation, TextureAtlasSprite> spriteAtlas = Minecraft.getInstance()
                    .getTextureAtlas(InventoryMenu.BLOCK_ATLAS);
            IClientFluidTypeExtensions properties = IClientFluidTypeExtensions.of(getFluid());
            ResourceLocation resource = properties.getStillTexture();
            fluidTexture = spriteAtlas.apply(resource);
            int fcol = properties.getTintColor();
            fluidColorR = ((fcol >> 16) & 0xFF) / 255.0F;
            fluidColorG = ((fcol >> 8) & 0xFF) / 255.0F;
            fluidColorB = (fcol & 0xFF) / 255.0F;
            fluidColorA = ((fcol >> 24) & 0xFF) / 255.0F;
            textureWidth = fluidTexture.getWidth();
            textureHeight = fluidTexture.getHeight();
        }
    }
    public TankGauge(FluidTank tank, int x, int y, int width, int height) {
        super(x, y, width, height, Component.translatable("gui.adminshop.tank_gauge"));
        setTank(tank);
    }

    public void setTank(FluidTank tank) {
        this.tank = tank;
        setFluidTextures();
    }

    public IFluidTank getTank() {
        return this.tank;
    }

    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTicks) {
        super.render(poseStack, mouseX, mouseY, partialTicks);
    }

    @Override
    public void renderToolTip(PoseStack pPoseStack, int pMouseX, int pMouseY) {
        super.renderToolTip(pPoseStack, pMouseX, pMouseY);
    }

    @Override
    protected void renderBg(PoseStack pPoseStack, Minecraft pMinecraft, int pMouseX, int pMouseY) {
        super.renderBg(pPoseStack, pMinecraft, pMouseX, pMouseY);
    }

    @Override
    public void renderButton(@NotNull PoseStack matrix, int mouseX, int mouseY, float partialTicks) {
        //super.renderButton(matrix, x, y, partialTicks);
        if(!visible) return;
        matrix.pushPose();
//        AdminShop.LOGGER.debug("Tank contents: "+tank.getFluid().getAmount()+"mb "+tank.getFluid().getDisplayName().getString());

        // Render Tank Contents
        if(!tank.getFluid().isEmpty()) {
            setFluidTextures();
            // Set render for fluid
            enableScissor(x, y, x + width, y + height);
            RenderSystem.enableBlend();
            RenderSystem.setShader(GameRenderer::getPositionTexShader);
            RenderSystem.setShaderTexture(0, fluidTexture.atlas().location());
            RenderSystem.setShaderColor(fluidColorR, fluidColorG, fluidColorB, fluidColorA);
            // Get height value proportional to filled capacity
            float pixelsPerMb = height / (float) tank.getCapacity();
            int filledHeight = (int) (pixelsPerMb * getQuantity());
            // Calculate the number of full tiles and the remainder
            int fullTiles = filledHeight / textureHeight;
            int remainderHeight = filledHeight % textureHeight;
            // Render tiled texture
            for(int j = 0; j < fullTiles; j++) {
                blit(matrix, x, y + height - ((j+1) * textureHeight), getBlitOffset() + 100, width, textureHeight, fluidTexture);
            }
            // Render remainder
            if (remainderHeight > 0) {
                blit(matrix, x, y + height - (fullTiles * textureHeight) - remainderHeight, getBlitOffset() + 100, width, remainderHeight, fluidTexture);
            }
            // Reset render
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
            RenderSystem.disableBlend();
            RenderSystem.disableScissor();
        }

        //Highlight background and write fluid name if hovered or focused
        //            fill(matrix, x, y, x+width, y+height, 0xFFFFFFDF);
        isMouseOn = isHoveredOrFocused();
        //Write fluid quantity
        matrix.pushPose();
        matrix.translate(0, 0, 201);
        matrix.scale(.5f, .5f, 1);
        Font font = Minecraft.getInstance().font;
        drawString(matrix, font, getQuantity()+"", 2*(x+16)- font.width(getQuantity()+""), 2*(y)+92, 0xFFFFFF);
        matrix.popPose();

        matrix.popPose();
    }

    public int getQuantity(){
        if (tank.getFluid().isEmpty()) return 0;
        return tank.getFluidAmount();
    }

    public List<Component> getTooltipContent(){
        List<Component> tootlip;
        if (tank.getFluid().isEmpty()) {
            tootlip = List.of(Component.literal("0/"+tank.getCapacity()),
                    Component.literal("Empty"));
        } else {
            tootlip = List.of(Component.literal(getQuantity()+"/"+tank.getCapacity()),
                    Component.literal(tank.getFluid().getDisplayName().getString()));
        }
        return tootlip;
    }

    @Override
    public void updateNarration(NarrationElementOutput pNarrationElementOutput) {
        return;
    }
}
