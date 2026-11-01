package com.ammonium.adminshop.screen;

import com.ammonium.adminshop.AdminShop;
import com.ammonium.adminshop.blocks.entity.FluidSellerEntity;
import com.ammonium.adminshop.client.gui.ProgressBar;
import com.ammonium.adminshop.client.gui.TankGauge;
import com.ammonium.adminshop.money.ClientCache;
import com.ammonium.adminshop.money.MoneyHelper;
import com.ammonium.adminshop.network.PacketUpdateRequest;
import com.ammonium.adminshop.setup.Messages;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import org.joml.Matrix3x2fStack;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.transfer.fluid.FluidStacksResourceHandler;

import java.util.Optional;
import java.util.UUID;

public class FluidSellerScreen extends AbstractContainerScreen<FluidSellerMenu> {
    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath(AdminShop.MODID, "textures/gui/fluid_seller.png");
    private final BlockPos blockPos;
    private FluidSellerEntity sellerEntity;
    private UUID teamId = null;
    private TankGauge tankGauge;
    private ProgressBar progressBar;

    public FluidSellerScreen(FluidSellerMenu pMenu, Inventory pPlayerInventory, Component pTitle, BlockPos blockPos) {
        super(pMenu, pPlayerInventory, pTitle);
        this.blockPos = blockPos;
    }

    @Override
    protected void init() {
        super.init();
        this.sellerEntity = this.getMenu().getBlockEntity();
        int relX = (this.width - this.imageWidth) / 2;
        int relY = (this.height - this.imageHeight) / 2;
        this.tankGauge = new TankGauge(this.sellerEntity.getTank(), relX+63, relY+10, 16, 50);
        addRenderableWidget(this.tankGauge);
        this.progressBar = new ProgressBar(relX + 88, relY + 30);
        addRenderableWidget(this.progressBar);

        // Request update from server
        Messages.sendToServer(new PacketUpdateRequest(this.blockPos));
    }
    private void updateInformation() {
        this.teamId = this.sellerEntity.getTeamId();
        this.tankGauge.setTank(this.sellerEntity.getTank());
        this.progressBar.setProgress(this.sellerEntity.getProgress());
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor guiGraphics, int pMouseX, int pMouseY, float pPartialTicks) {
        super.extractBackground(guiGraphics, pMouseX, pMouseY, pPartialTicks);
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, 0, 0, imageWidth, imageHeight, 256, 256);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor guiGraphics, int pMouseX, int pMouseY) {
        super.extractLabels(guiGraphics, pMouseX, pMouseY);
        Component name = Component.translatable("gui.adminshop.no_account");
        boolean accAvailable = false;
        MoneyHelper.MoneyAccount account = ClientCache.getAccount();
        if (account != null) {
            name = account.name();
            accAvailable = true;
        }
        int color = accAvailable ? 0xffffffff : 0xffff0000;
        guiGraphics.text(font, name.getString(), 7,62,color);
        Matrix3x2fStack poseStack = guiGraphics.pose();
        poseStack.pushMatrix();
        if (this.tankGauge == null) {
            AdminShop.LOGGER.debug("TankGauge is null!");
        }
        if (this.tankGauge != null && tankGauge.isMouseOn) {
            guiGraphics.setTooltipForNextFrame(font, tankGauge.getTooltipContent(),
                    Optional.empty(), pMouseX,
                    pMouseY);
        }
        poseStack.popMatrix();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(guiGraphics, mouseX, mouseY, delta);

        // Get data from BlockEntity
        this.sellerEntity = this.getMenu().getBlockEntity();

        UUID teamId = this.sellerEntity.getTeamId();
        FluidStacksResourceHandler buyerTank = this.sellerEntity.getTank();

        boolean shouldUpdateDueToNulls =
                (this.teamId == null && teamId != null) ||
                (this.tankGauge.getTank() == null && buyerTank != null);

        boolean shouldUpdateDueToDifferences =
                (this.teamId != null && !this.teamId.equals(teamId)) ||
                (!this.tankGauge.getTank().equals(buyerTank)) ||
                (this.sellerEntity.getProgress() != this.progressBar.getProgress());

        if (shouldUpdateDueToNulls || shouldUpdateDueToDifferences) {
            updateInformation();
        }
    }
}
