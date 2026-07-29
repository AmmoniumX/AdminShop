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
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.UUID;

public class AbstractBuyerScreen<T extends AbstractBuyerMenu> extends AbstractContainerScreen<T> {
    private final ResourceLocation TEXTURE;
    private final BlockPos blockPos;
    private AbstractBuyerEntity buyerEntity;
    private UUID teamId = null;
    private BuyItemRecipe recipe;
    private ProgressBar progressBar;

    public AbstractBuyerScreen(String texturePath, T pMenu, Inventory pPlayerInventory, Component pTitle, BlockPos blockPos) {
        super(pMenu, pPlayerInventory, pTitle);
        this.blockPos = blockPos;
        this.TEXTURE = ResourceLocation.fromNamespaceAndPath(AdminShop.MODID, texturePath);
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
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
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
                    return super.mouseClicked(mouseX, mouseY, button);
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
                    this.buyerEntity.forceSetRecipe(recipeHolder.id());
                    this.recipe = recipeHolder.value();
                    Messages.sendToServer(new PacketSetItemBuyerRecipe(this.blockPos, recipeHolder.id()));
                    return false;
                }
                BuyItemRecipe recipe = recipeHolder.value();
                // Set buyer target
                // Check if account has permit to buy item
                if (ClientCache.hasPermit(recipe.getPermit())) {
                    this.buyerEntity.setRecipe(recipeHolder.id());
                    this.recipe = recipe;
                    Messages.sendToServer(new PacketSetItemBuyerRecipe(this.blockPos, recipeHolder.id()));
                    return false;
                } else {
                    LocalPlayer player = Minecraft.getInstance().player;
                    assert player != null;
                    player.sendSystemMessage(Component.translatable("gui.adminshop.no_permit"));
                    return false;
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
        if (this.recipe != null) {
            ItemStack item = this.recipe.getItem().orElseThrow();
            renderTargetItem(guiGraphics, item, x, y);
        }
    }

    private void renderTargetItem(GuiGraphics guiGraphics, ItemStack item, int x, int y) {
        assert this.minecraft != null;
        ItemRenderer itemRenderer = this.minecraft.getItemRenderer();
//        itemRenderer.renderAndDecorateFakeItem(item, x+104, y+14);
        guiGraphics.renderFakeItem(item, x+104, y+14);
        if (!item.getComponentsPatch().isEmpty()) {
//            poseStack.pushPose();

//            poseStack.translate(x + 104, y + 16, guiGraphics.+200);
//            poseStack.scale(0.5F, 0.5F, 1.0F);
//            drawString(poseStack, font, "+NBT", 0, 0, 0xFF55FF);

//            poseStack.popPose();
            // Render scaled text
            PoseStack poseStack = guiGraphics.pose();
            poseStack.pushPose();

            poseStack.translate(x + 104, y + 16, 200);
            poseStack.scale(0.5F, 0.5F, 1.0F);

            // When using scaled text, you need to use coordinates relative to the translation point (0, 0)
            guiGraphics.drawString(font, "+NBT", 0, 0, 0xFF55FF);

            poseStack.popPose();
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);
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
        renderBackground(guiGraphics, mouseX, mouseY, delta);
        super.render(guiGraphics, mouseX, mouseY, delta);
        renderTooltip(guiGraphics, mouseX, mouseY);

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
