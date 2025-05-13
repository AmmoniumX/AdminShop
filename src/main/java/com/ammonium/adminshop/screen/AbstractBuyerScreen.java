package com.ammonium.adminshop.screen;

import com.ammonium.adminshop.AdminShop;
import com.ammonium.adminshop.blocks.entity.AbstractBuyerEntity;
import com.ammonium.adminshop.money.ClientCache;
import com.ammonium.adminshop.money.MoneyHelper;
import com.ammonium.adminshop.network.PacketSetItemBuyerRecipe;
import com.ammonium.adminshop.network.PacketUpdateRequest;
import com.ammonium.adminshop.recipes.BuyItemRecipe;
import com.ammonium.adminshop.recipes.RecipeManager;
import com.ammonium.adminshop.setup.Messages;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.UUID;

public class AbstractBuyerScreen<T extends AbstractBuyerMenu> extends AbstractContainerScreen<T> {
    private final ResourceLocation TEXTURE;
    private final BlockPos blockPos;
    private AbstractBuyerEntity buyerEntity;
    private UUID teamId = null;
    private BuyItemRecipe recipe;

    public AbstractBuyerScreen(String texturePath, T pMenu, Inventory pPlayerInventory, Component pTitle, BlockPos blockPos) {
        super(pMenu, pPlayerInventory, pTitle);
        this.blockPos = blockPos;
        this.TEXTURE = new ResourceLocation(AdminShop.MODID, texturePath);
    }

    @Override
    protected void init() {
        super.init();

        // Request update from server
        Messages.sendToServer(new PacketUpdateRequest(this.blockPos));
    }
    private void updateInformation(Level level) {
        this.teamId = this.buyerEntity.getTeamId();
        this.recipe = this.buyerEntity.getRecipe(level).orElse(null);
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
                BuyItemRecipe recipe = RecipeManager.isBuyItemRecipe(Minecraft.getInstance().level, itemStack).orElse(null);
                // Return super if not in buy map
                if (recipe == null) {
                    AdminShop.LOGGER.debug("Item not in buy recipes: {}", itemStack.getDisplayName().getString());
                    return super.mouseClicked(mouseX, mouseY, button);
                }
                // Set buyer target
                // Check if account has permit to buy item
                if (ClientCache.hasPermit(recipe.getPermit())) {
                    this.buyerEntity.setRecipe(recipe.getId());
                    this.recipe = recipe;
                    Messages.sendToServer(new PacketSetItemBuyerRecipe(this.blockPos, this.recipe.getId()));
                    return false;
                } else {
                    LocalPlayer player = Minecraft.getInstance().player;
                    assert player != null;
                    player.sendSystemMessage(Component.literal("You haven't unlocked that yet!"));
                    return false;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected void renderBg(PoseStack poseStack, float ppartialticks, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, TEXTURE);
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        this.blit(poseStack, x, y, 0, 0, imageWidth, imageHeight);
        if (this.recipe != null) {
            renderItem(poseStack, this.recipe.getItem().get().getItem(), x+104, y+14);
            if (this.recipe.getItem().get().hasTag()) {
                drawString(poseStack, font, "+NBT", x+104-font.width("+NBT")-1, y+14, 0xFF55FF);
            }
        }
    }

    @Override
    protected void renderLabels(PoseStack poseStack, int mouseX, int mouseY) {
        super.renderLabels(poseStack, mouseX, mouseY);
        Component name = Component.literal("No account");
        boolean accAvailable = false;
        MoneyHelper.MoneyAccount account = ClientCache.getAccount();
        if (account != null) {
            name = account.name();
            accAvailable = true;
        }
        int color = accAvailable ? 0xffffff : 0xff0000;
        drawString(poseStack, font, name.getString(), 7,62,color);
    }

    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float delta) {
        renderBackground(poseStack);
        super.render(poseStack, mouseX, mouseY, delta);
        renderTooltip(poseStack, mouseX, mouseY);

        // Get data from BlockEntity
        this.buyerEntity = this.getMenu().getBlockEntity();

        UUID teamId = this.buyerEntity.getTeamId();
        BuyItemRecipe recipe = this.buyerEntity.getRecipe(Minecraft.getInstance().level).orElse(null);

        boolean shouldUpdateDueToNulls = (this.teamId == null && teamId != null) ||
                (this.recipe == null && recipe != null);

        boolean shouldUpdateDueToDifferences = (this.teamId != null && !this.teamId.equals(teamId)) ||
                (this.recipe != recipe);

        if (shouldUpdateDueToNulls || shouldUpdateDueToDifferences) {
            updateInformation(Minecraft.getInstance().level);
        }
    }

    private void renderItem(PoseStack matrixStack, Item item, int x, int y) {
        ItemRenderer itemRenderer = this.minecraft.getItemRenderer();
        ItemStack itemStack = new ItemStack(item);
        itemRenderer.renderAndDecorateFakeItem(itemStack, x, y);
    }
}
