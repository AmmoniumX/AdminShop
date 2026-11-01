package com.ammonium.adminshop.screen;

import com.ammonium.adminshop.AdminShop;
import com.ammonium.adminshop.blocks.entity.AbstractBuyerEntity;
import com.ammonium.adminshop.client.gui.ProgressBar;
import com.ammonium.adminshop.money.ClientCache;
import com.ammonium.adminshop.money.MoneyHelper;
import com.ammonium.adminshop.network.PacketSetItemBuyerRecipe;
import com.ammonium.adminshop.network.PacketUpdateRequest;
import com.ammonium.adminshop.recipes.BuyItemRecipe;
import com.ammonium.adminshop.recipes.RecipeManager;
import net.minecraft.world.item.crafting.RecipeHolder;
import com.ammonium.adminshop.setup.Messages;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.client.input.MouseButtonEvent;
import org.joml.Matrix3x2fStack;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.UUID;

public class AbstractBuyerScreen<T extends AbstractBuyerMenu> extends AbstractContainerScreen<T> {
    private final Identifier TEXTURE;
    private final BlockPos blockPos;
    private AbstractBuyerEntity buyerEntity;
    private UUID teamId = null;
    private BuyItemRecipe recipe;
    private ProgressBar progressBar;

    public AbstractBuyerScreen(String texturePath, T pMenu, Inventory pPlayerInventory, Component pTitle, BlockPos blockPos) {
        super(pMenu, pPlayerInventory, pTitle);
        this.blockPos = blockPos;
        this.TEXTURE = Identifier.fromNamespaceAndPath(AdminShop.MODID, texturePath);
    }

    @Override
    protected void init() {
        super.init();
        int relX = (this.width - this.imageWidth) / 2;
        int relY = (this.height - this.imageHeight) / 2;
        this.progressBar = new ProgressBar(relX + 77, relY + 15);
        addRenderableWidget(this.progressBar);

        // Request update from server
        Messages.sendToServer(new PacketUpdateRequest(this.blockPos));
    }
    private void updateInformation(Level level) {
        this.teamId = this.buyerEntity.getTeamId();
        this.recipe = this.buyerEntity.getRecipe(level).orElse(null);
        this.progressBar.setProgress(this.buyerEntity.getProgress());
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        Slot slot = this.getSlotUnderMouse();
        if (slot != null) {
            ItemStack itemStack = slot.getItem();
            boolean isMachineSlot = slot.index >= this.menu.getTeInventoryFirstSlotIndex() && slot.index < this.menu
                    .getTeInventoryFirstSlotIndex() + this.menu.getTeInventorySlotCount();
            if (!itemStack.isEmpty() && !isMachineSlot) {
                // Get item clicked on
                AdminShop.LOGGER.debug("Clicked on item: {}", itemStack.getDisplayName().getString());
                RecipeHolder<BuyItemRecipe> recipeHolder = RecipeManager.isBuyItemRecipe(Minecraft.getInstance().level, itemStack).orElse(null);
                // Return super if not in buy map
                if (recipeHolder == null) {
                    AdminShop.LOGGER.debug("Item not in buy recipes: {}", itemStack.getDisplayName().getString());
                    return super.mouseClicked(event, doubleClick);
                }
                LocalPlayer clickingPlayer = Minecraft.getInstance().player;
                assert clickingPlayer != null;
                if (this.buyerEntity.isLockedRecipe()) {
                    if (!clickingPlayer.isCreative()) {
                        // Recipe is locked server-side; don't touch the client's view of the target item.
                        clickingPlayer.sendSystemMessage(Component.translatable("message.adminshop.recipe_locked"));
                        return false;
                    }
                    // Creative players can still configure a locked machine's recipe.
                    this.buyerEntity.forceSetRecipe(recipeHolder.id().identifier());
                    this.recipe = recipeHolder.value();
                    Messages.sendToServer(new PacketSetItemBuyerRecipe(this.blockPos, recipeHolder.id().identifier()));
                    return false;
                }
                BuyItemRecipe recipe = recipeHolder.value();
                // Set buyer target
                // Check if account has permit to buy item
                if (ClientCache.hasPermit(recipe.getPermit())) {
                    this.buyerEntity.setRecipe(recipeHolder.id().identifier());
                    this.recipe = recipe;
                    Messages.sendToServer(new PacketSetItemBuyerRecipe(this.blockPos, recipeHolder.id().identifier()));
                    return false;
                } else {
                    LocalPlayer player = Minecraft.getInstance().player;
                    assert player != null;
                    player.sendSystemMessage(Component.translatable("gui.adminshop.no_permit"));
                    return false;
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
        if (this.recipe != null) {
            ItemStack item = this.recipe.getItem().orElseThrow();
            renderTargetItem(guiGraphics, item, x, y);
        }
    }

    private void renderTargetItem(GuiGraphicsExtractor guiGraphics, ItemStack item, int x, int y) {
        assert this.minecraft != null;
//        itemRenderer.renderAndDecorateFakeItem(item, x+104, y+14);
        guiGraphics.fakeItem(item, x+104, y+14);
        if (!item.getComponentsPatch().isEmpty()) {
//            poseStack.pushMatrix();

//            poseStack.translate(x + 104, y + 16, guiGraphics.+200);
//            poseStack.scale(0.5F, 0.5F, 1.0F);
//            drawString(poseStack, font, "+NBT", 0, 0, 0xFF55FF);

//            poseStack.popMatrix();
            // Render scaled text
            Matrix3x2fStack poseStack = guiGraphics.pose();
            poseStack.pushMatrix();

            poseStack.translate(x + 104, y + 16);
            poseStack.scale(0.5F, 0.5F);

            // When using scaled text, you need to use coordinates relative to the translation point (0, 0)
            guiGraphics.text(font, "+NBT", 0, 0, 0xFFFF55FF);

            poseStack.popMatrix();
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
        super.extractLabels(guiGraphics, mouseX, mouseY);
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
        this.buyerEntity = this.getMenu().getBlockEntity();

        UUID teamId = this.buyerEntity.getTeamId();
        BuyItemRecipe recipe = this.buyerEntity.getRecipe(Minecraft.getInstance().level).orElse(null);

        boolean shouldUpdateDueToNulls = (this.teamId == null && teamId != null) ||
                (this.recipe == null && recipe != null);

        boolean shouldUpdateDueToDifferences = (this.teamId != null && !this.teamId.equals(teamId)) ||
                (this.recipe != recipe) ||
                (this.buyerEntity.getProgress() != this.progressBar.getProgress());

        if (shouldUpdateDueToNulls || shouldUpdateDueToDifferences) {
            updateInformation(Minecraft.getInstance().level);
        }
    }

}
