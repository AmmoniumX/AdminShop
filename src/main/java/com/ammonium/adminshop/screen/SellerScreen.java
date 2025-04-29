package com.ammonium.adminshop.screen;

import com.ammonium.adminshop.AdminShop;
import com.ammonium.adminshop.blocks.entity.SellerBE;
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

import java.util.UUID;

public class SellerScreen extends AbstractContainerScreen<SellerMenu> {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(AdminShop.MODID, "textures/gui/seller.png");
    private final BlockPos blockPos;
    private SellerBE sellerEntity;
    private UUID teamId = null;

    public SellerScreen(SellerMenu pMenu, Inventory pPlayerInventory, Component pTitle, BlockPos blockPos) {
        super(pMenu, pPlayerInventory, pTitle);
        this.blockPos = blockPos;
    }

    @Override
    protected void init() {
        super.init();
        int relX = (this.width - this.imageWidth) / 2;
        int relY = (this.height - this.imageHeight) / 2;

        // Request update from server
//        System.out.println("Requesting update from server");
        Messages.sendToServer(new PacketUpdateRequest(this.blockPos));
    }
    private void updateInformation() {
        this.teamId = this.sellerEntity.getTeamId();
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
        Component name = Component.literal("No account");
        boolean accAvailable = false;
        MoneyHelper.MoneyAccount account = ClientCache.getAccount();
        if (account != null) {
            name = account.name();
            accAvailable = true;
        }
        int color = accAvailable ? 0xffffff : 0xff0000;
        drawString(pPoseStack, font, name.getString(), 7,62,color);
    }

    @Override
    public void render(PoseStack pPoseStack, int mouseX, int mouseY, float delta) {
        renderBackground(pPoseStack);
        super.render(pPoseStack, mouseX, mouseY, delta);
        renderTooltip(pPoseStack, mouseX, mouseY);

        // Get data from BlockEntity
        this.sellerEntity = this.getMenu().getBlockEntity();

        if (this.teamId == null) {
            if (this.sellerEntity.getTeamId() != null) {
                updateInformation();
            }
        }
        if (this.teamId != null && (!this.teamId.equals(this.sellerEntity.getTeamId()))) {
            updateInformation();
        }
    }
}
