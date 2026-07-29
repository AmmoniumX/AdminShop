package com.ammonium.adminshop.screen;

import com.ammonium.adminshop.AdminShop;
import com.ammonium.adminshop.blocks.entity.SellerEntity;
import com.ammonium.adminshop.client.gui.ProgressBar;
import com.ammonium.adminshop.money.ClientCache;
import com.ammonium.adminshop.money.MoneyHelper;
import com.ammonium.adminshop.network.PacketSetSellerRecipe;
import com.ammonium.adminshop.network.PacketUpdateRequest;
import com.ammonium.adminshop.recipes.RecipeManager;
import com.ammonium.adminshop.recipes.SellItemRecipe;
import com.ammonium.adminshop.setup.Messages;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

public class SellerScreen<T extends SellerMenu> extends AbstractContainerScreen<T> {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(AdminShop.MODID, "textures/gui/seller.png");
    private final BlockPos blockPos;
    private SellerEntity sellerEntity;
    private UUID teamId = null;
    private ProgressBar progressBar;

    public SellerScreen(T pMenu, Inventory pPlayerInventory, Component pTitle, BlockPos blockPos) {
        super(pMenu, pPlayerInventory, pTitle);
        this.blockPos = blockPos;
    }

    @Override
    protected void init() {
        super.init();
        int relX = (this.width - this.imageWidth) / 2;
        int relY = (this.height - this.imageHeight) / 2;
        this.progressBar = new ProgressBar(relX + 80, relY + 30);
        addRenderableWidget(this.progressBar);

        // Request update from server
        Messages.sendToServer(new PacketUpdateRequest(this.blockPos));
    }
    private void updateInformation() {
        this.teamId = this.sellerEntity.getTeamId();
        this.progressBar.setProgress(this.sellerEntity.getProgress());
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Creative players can lock this machine to a specific sell recipe by clicking
        // an item in their own inventory, mirroring how Buyers pick their target item.
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null && player.isCreative()) {
            Slot slot = this.getSlotUnderMouse();
            if (slot != null) {
                ItemStack itemStack = slot.getItem();
                boolean isMachineSlot = slot.index >= this.menu.getTeInventoryFirstSlotIndex()
                        && slot.index < this.menu.getTeInventoryFirstSlotIndex() + this.menu.getTeInventorySlotCount();
                if (!itemStack.isEmpty() && !isMachineSlot) {
                    SellItemRecipe recipe =
                            RecipeManager.isSellItemRecipe(Minecraft.getInstance().level, itemStack).orElse(null);
                    if (recipe != null) {
                        this.sellerEntity.setLockedRecipeId(recipe.getId());
                        Messages.sendToServer(new PacketSetSellerRecipe(this.blockPos, recipe.getId()));
                        return false;
                    }
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float pPartialTicks, int pMouseX, int pMouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, TEXTURE);
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        guiGraphics.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int pMouseX, int pMouseY) {
        super.renderLabels(guiGraphics, pMouseX, pMouseY);
        Component name = Component.translatable("gui.adminshop.no_account");
        boolean accAvailable = false;
        if (this.teamId != null) {
            MoneyHelper.MoneyAccount account = ClientCache.getAccount();
            if (account != null) {
                name = account.name();
                accAvailable = true;
            }
        }
        int color = accAvailable ? 0xffffff : 0xff0000;
        guiGraphics.drawString(font, name.getString(), 7,62,color);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, delta);
        renderTooltip(guiGraphics, mouseX, mouseY);

        // Get data from BlockEntity
        this.sellerEntity = this.getMenu().getBlockEntity();
        updateInformation();
    }
}
