package com.ammonium.adminshop.client.gui;

import com.ammonium.adminshop.AdminShop;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
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
        }
    }
    public TankGauge(FluidTank tank, int x, int y, int width, int height) {
        super(x, y, width, height, Component.literal("Tank Gauge"));
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
    public void render(PoseStack pPoseStack, int pMouseX, int pMouseY, float pPartialTick) {
        super.render(pPoseStack, pMouseX, pMouseY, pPartialTick);
    }

    @Override
    public void renderToolTip(PoseStack pPoseStack, int pMouseX, int pMouseY) {
        AdminShop.LOGGER.debug("Render Tooltip");
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

        // Render Fluid
        if(!tank.getFluid().isEmpty()) {
            setFluidTextures();
            RenderSystem.bindTexture(fluidTexture.atlas().getId());
            RenderSystem.setShaderColor(fluidColorR, fluidColorG, fluidColorB, fluidColorA);
            RenderSystem.setShaderTexture(0,
                    fluidTexture.atlas().location());
            float pixelsPerMb = height / (float) tank.getCapacity();
            int filledHeight = (int) (pixelsPerMb * getQuantity());
            blit(matrix, x, y+(height-filledHeight),0, width, filledHeight, fluidTexture);
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
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
