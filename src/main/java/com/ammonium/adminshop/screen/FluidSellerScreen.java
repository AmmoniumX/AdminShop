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
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.fluids.capability.templates.FluidTank;

import java.util.Optional;
import java.util.UUID;

public class FluidSellerScreen extends AbstractContainerScreen<FluidSellerMenu> {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(AdminShop.MODID, "textures/gui/fluid_seller.png");
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
    protected void renderBg(PoseStack pPoseStack, float pPartialTicks, int pMouseX, int pMouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, TEXTURE);
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        this.blit(pPoseStack, x, y, 0, 0, imageWidth, imageHeight);
    }

    @Override
    protected void renderLabels(PoseStack pPoseStack, int pMouseX, int pMouseY) {
        super.renderLabels(pPoseStack, pMouseX, pMouseY);
        Component name = Component.translatable("gui.adminshop.no_account");
        boolean accAvailable = false;
        MoneyHelper.MoneyAccount account = ClientCache.getAccount();
        if (account != null) {
            name = account.name();
            accAvailable = true;
        }
        int color = accAvailable ? 0xffffff : 0xff0000;
        drawString(pPoseStack, font, name.getString(), 7,62,color);
        pPoseStack.pushPose();
        if (this.tankGauge == null) {
            AdminShop.LOGGER.debug("TankGauge is null!");
        }
        if (this.tankGauge != null && tankGauge.isMouseOn) {
            renderTooltip(pPoseStack, tankGauge.getTooltipContent(),
                    Optional.empty(), pMouseX-(this.width - this.imageWidth)/2,
                    pMouseY-(this.height - this.imageHeight)/2);
        }
        pPoseStack.popPose();
    }

    @Override
    public void render(PoseStack pPoseStack, int mouseX, int mouseY, float delta) {
        renderBackground(pPoseStack);
        super.render(pPoseStack, mouseX, mouseY, delta);
        renderTooltip(pPoseStack, mouseX, mouseY);

        // Get data from BlockEntity
        this.sellerEntity = this.getMenu().getBlockEntity();

        UUID teamId = this.sellerEntity.getTeamId();
        FluidTank buyerTank = this.sellerEntity.getTank();

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
