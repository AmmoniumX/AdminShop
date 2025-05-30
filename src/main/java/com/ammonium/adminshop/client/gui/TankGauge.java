package com.ammonium.adminshop.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureManager;
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
    private int fluidTextureId;
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
            TextureManager manager = Minecraft.getInstance().getTextureManager();
            AbstractTexture abstractTexture = manager.getTexture(InventoryMenu.BLOCK_ATLAS);
            TextureAtlas atlas = null;
            if (abstractTexture instanceof TextureAtlas) {
                atlas = (TextureAtlas) abstractTexture;
            }
            assert atlas != null;
            fluidTextureId = atlas.getId();
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
    public void render(GuiGraphics guiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        super.render(guiGraphics, pMouseX, pMouseY, pPartialTick);
    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        int x = getX();
        int y = getY();
        PoseStack matrix = guiGraphics.pose();
        //super.renderButton(matrix, x, y, partialTicks);
        if(!visible) return;
        matrix.pushPose();
//        AdminShop.LOGGER.debug("Tank contents: "+tank.getFluid().getAmount()+"mb "+tank.getFluid().getDisplayName().getString());

        // Render Fluid
        if(!tank.getFluid().isEmpty()) {
            setFluidTextures();
            RenderSystem.bindTexture(fluidTextureId);
            RenderSystem.setShaderColor(fluidColorR, fluidColorG, fluidColorB, fluidColorA);
            RenderSystem.setShaderTexture(0,
                    fluidTexture.atlasLocation());
            float pixelsPerMb = height / (float) tank.getCapacity();
            int filledHeight = (int) (pixelsPerMb * getQuantity());
            guiGraphics.blit(x, y+(height-filledHeight),0, width, filledHeight, fluidTexture);
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        }

        //Highlight background and write fluid name if hovered or focused
        //            fill(matrix, x, y, x+width, y+height, 0xFFFFFFDF);
        isMouseOn = isHoveredOrFocused();
        //Write fluid quantity
        matrix.pushPose();
        matrix.translate(0, 0, 201);
        matrix.scale(.5f, .5f, 1);
        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;
        guiGraphics.drawString(font, getQuantity()+"", 2*(x+16)- font.width(getQuantity()+""), 2*(y)+92, 0xFFFFFF);
        matrix.popPose();

        matrix.popPose();
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput pNarrationElementOutput) {
        return;
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
}