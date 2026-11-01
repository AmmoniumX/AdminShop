package com.ammonium.adminshop.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.IFluidTank;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidStacksResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidUtil;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix3x2fStack;

import java.util.List;
import java.util.function.Function;

public class TankGauge extends AbstractWidget {

    private FluidStacksResourceHandler tank;
    private TextureAtlasSprite fluidTexture;
    private float fluidColorR, fluidColorG, fluidColorB, fluidColorA;
    public boolean isMouseOn = false;

    private Fluid getFluid() {
        if (FluidUtil.getStack(tank, 0).isEmpty()) {return null;}
        return FluidUtil.getStack(tank, 0).getFluid();
    }

    private void setFluidTextures() {
        if(!FluidUtil.getStack(tank, 0).isEmpty()) {
            FluidModel properties = Minecraft.getInstance().getModelManager().getFluidStateModelSet()
                    .get(getFluid().defaultFluidState());
            fluidTexture = properties.stillMaterial().sprite();
            int fcol = properties.fluidTintSource() != null ? properties.fluidTintSource().colorAsStack(FluidUtil.getStack(tank, 0)) : -1;
            if (ARGB.alpha(fcol) == 0) { fcol = ARGB.opaque(fcol); }
            fluidColorR = ((fcol >> 16) & 0xFF) / 255.0F;
            fluidColorG = ((fcol >> 8) & 0xFF) / 255.0F;
            fluidColorB = (fcol & 0xFF) / 255.0F;
            fluidColorA = ((fcol >> 24) & 0xFF) / 255.0F;
        }
    }
    public TankGauge(FluidStacksResourceHandler tank, int x, int y, int width, int height) {
        super(x, y, width, height, Component.literal("Tank Gauge"));
        setTank(tank);
    }

    public void setTank(FluidStacksResourceHandler tank) {
        this.tank = tank;
        setFluidTextures();
    }

    public FluidStacksResourceHandler getTank() {
        return this.tank;
    }

    @Override
    public void extractWidgetRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTicks) {
        int x = getX();
        int y = getY();
        Matrix3x2fStack matrix = guiGraphics.pose();
        //super.renderButton(matrix, x, y, partialTicks);
        if(!visible) return;
        matrix.pushMatrix();
//        AdminShop.LOGGER.debug("Tank contents: "+tank.getFluid().getAmount()+"mb "+tank.getFluid().getDisplayName().getString());

        // Render Fluid
        if(!FluidUtil.getStack(tank, 0).isEmpty()) {
            setFluidTextures();
            float pixelsPerMb = height / (float) tank.getCapacityAsInt(0, FluidResource.EMPTY);
            int filledHeight = (int) (pixelsPerMb * getQuantity());
            guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, fluidTexture, x, y+(height-filledHeight), width, filledHeight,
                    ARGB.colorFromFloat(fluidColorA, fluidColorR, fluidColorG, fluidColorB));
        }

        //Highlight background and write fluid name if hovered or focused
        //            fill(matrix, x, y, x+width, y+height, 0xFFFFFFDF);
        isMouseOn = isHoveredOrFocused();
        //Write fluid quantity
        matrix.pushMatrix();
        matrix.scale(.5f, .5f);
        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;
        guiGraphics.text(font, getQuantity()+"", 2*(x+16)- font.width(getQuantity()+""), 2*(y)+92, 0xFFFFFFFF);
        matrix.popMatrix();

        matrix.popMatrix();
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput pNarrationElementOutput) {
        return;
    }

    public int getQuantity(){
        if (FluidUtil.getStack(tank, 0).isEmpty()) return 0;
        return tank.getAmountAsInt(0);
    }

    public List<Component> getTooltipContent(){
        List<Component> tootlip;
        if (FluidUtil.getStack(tank, 0).isEmpty()) {
            tootlip = List.of(Component.literal("0/"+tank.getCapacityAsInt(0, FluidResource.EMPTY)),
                    Component.literal("Empty"));
        } else {
            tootlip = List.of(Component.literal(getQuantity()+"/"+tank.getCapacityAsInt(0, FluidResource.EMPTY)),
                    Component.literal(FluidUtil.getStack(tank, 0).getHoverName().getString()));
        }
        return tootlip;
    }
}