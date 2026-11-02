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
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.UUID;

public class SellerScreen<T extends SellerMenu> extends AbstractContainerScreen<T> {
    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath(AdminShop.MODID, "textures/gui/seller.png");
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
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
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
                    RecipeHolder<SellItemRecipe> recipeHolder =
                            RecipeManager.isSellItemRecipe(Minecraft.getInstance().level, itemStack).orElse(null);
                    if (recipeHolder != null) {
                        this.sellerEntity.setLockedRecipeId(recipeHolder.id().identifier());
                        Messages.sendToServer(new PacketSetSellerRecipe(this.blockPos, recipeHolder.id().identifier()));
                        return false;
                    }
                }
            }
        }
        return super.mouseClicked(event, doubleClick);
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
        if (this.teamId != null) {
            MoneyHelper.MoneyAccount account = ClientCache.getAccount();
            if (account != null) {
                name = account.name();
                accAvailable = true;
            }
        }
        int color = accAvailable ? 0xffffffff : 0xffff0000;
        guiGraphics.text(font, name.getString(), 7,62,color);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(guiGraphics, mouseX, mouseY, delta);

        // Get data from BlockEntity
        this.sellerEntity = this.getMenu().getBlockEntity();
        updateInformation();
    }
}
