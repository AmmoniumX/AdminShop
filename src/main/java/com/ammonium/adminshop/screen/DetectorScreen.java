package com.ammonium.adminshop.screen;

import com.ammonium.adminshop.AdminShop;
import com.ammonium.adminshop.blocks.interfaces.Detector;
import com.ammonium.adminshop.client.gui.TextConfirmButton;
import com.ammonium.adminshop.money.ClientCache;
import com.ammonium.adminshop.money.MoneyHelper;
import com.ammonium.adminshop.network.PacketSetDetectorThreshold;
import com.ammonium.adminshop.network.PacketUpdateRequest;
import com.ammonium.adminshop.setup.Messages;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.UUID;
import java.util.function.Predicate;

public abstract class DetectorScreen<T extends DetectorMenu<Q>, Q extends Detector> extends AbstractContainerScreen<T> {
    private final Class<Q> detectorClass;
    private final BlockPos blockPos;
    private Q detectorBE;
    private UUID teamId = null;
    private long threshold;
    private TextConfirmButton textConfirmButton;
    private EditBox thresholdInputBox;

    public DetectorScreen(T pMenu, Inventory pPlayerInventory, Component pTitle, BlockPos blockPos, Class<Q> pClass) {
        super(pMenu, pPlayerInventory, pTitle);
        this.blockPos = blockPos;
        this.detectorClass = pClass;
        if (!pClass.isInstance(pMenu.getBlockEntity())) {
            throw new IllegalArgumentException("Invalid detector block entity type");
        }
        this.detectorBE = pClass.cast(pMenu.getBlockEntity());
        this.threshold = this.detectorBE.getThreshold();
        this.inventoryLabelY = 60;
    }

    protected abstract ResourceLocation getTexture();

    public DetectorScreen(T pMenu, Inventory inventory, Component pTitle, Class<Q> pClass) {
        this(pMenu, inventory, pTitle, ((BlockEntity) pMenu.getBlockEntity()).getBlockPos(), pClass);
    }

    private void createThresholdInputBox(int x, int y) {
        int boxWidth = 121;
        int boxHeight = 12;
        this.thresholdInputBox = new EditBox(font, x+38, y+24, boxWidth, boxHeight, Component.empty());
        this.thresholdInputBox.setValue(Long.toString(this.threshold));

        // Only accept numerical input
        this.thresholdInputBox.setFilter(new NumericalInputFilter());

        addRenderableWidget(this.thresholdInputBox);
    }

    private static class NumericalInputFilter implements Predicate<String> {
        @Override
        public boolean test(String input) {
            // Only allow numerical characters
            return input.matches("[0-9]*");
        }
    }
    private boolean isValidInput() {
        String currInput = this.thresholdInputBox.getValue();
        try {
            long value = Long.parseLong(currInput);
            return value >= 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    private void setThreshold() {
        String currInput = this.thresholdInputBox.getValue();
        long value;
        try {
            value = Long.parseLong(currInput);
            if (value < 0) return;
        } catch (NumberFormatException e) {
            return;
        }
        // Send packet to server
        AdminShop.LOGGER.debug("Setting detector threshold to "+value);
        Messages.sendToServer(new PacketSetDetectorThreshold(this.blockPos, value));
        this.detectorBE.setThreshold(value);
        if (Minecraft.getInstance().player != null) {
            Minecraft.getInstance().player.sendSystemMessage(Component.translatable("gui.adminshop.set_threshold", value));
        }
    }
    private void createTextConfirmButton(int x, int y) {
        if(textConfirmButton != null) {
            removeWidget(textConfirmButton);
        }
        textConfirmButton = new TextConfirmButton(x+159, y+24, (b) -> {
            Player player = Minecraft.getInstance().player;
            assert player != null;
            // Set threshold
            setThreshold();
        });
        addRenderableWidget(textConfirmButton);
    }

    @Override
    protected void init() {
        super.init();
        int relX = (this.width - this.imageWidth) / 2;
        int relY = (this.height - this.imageHeight) / 2;
        createThresholdInputBox(relX, relY);
        createTextConfirmButton(relX, relY);

        // Request update from server
        Messages.sendToServer(new PacketUpdateRequest(this.blockPos));
    }
    private void updateInformation() {
        this.teamId = this.detectorBE.getTeamId();
        this.threshold = this.detectorBE.getThreshold();
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float pPartialTicks, int pMouseX, int pMouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, getTexture());
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        guiGraphics.blit(getTexture(), x, y, 0, 0, imageWidth, imageHeight);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int pMouseX, int pMouseY) {
        super.renderLabels(guiGraphics, pMouseX, pMouseY);
        Component name = Component.translatable("gui.adminshop.no_account");
        boolean accAvailable = false;
        MoneyHelper.MoneyAccount account = ClientCache.getAccount();
        if (account != null) {
            name = account.name();
            accAvailable = true;
        }
        int color = accAvailable ? 0xffffff : 0xff0000;
        guiGraphics.drawString(font, name.getString(), 7,48,color);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        renderBackground(guiGraphics, mouseX, mouseY, delta);
        super.render(guiGraphics, mouseX, mouseY, delta);
        renderTooltip(guiGraphics, mouseX, mouseY);

        // Get data from BlockEntity
        this.detectorBE = this.detectorClass.cast(this.getMenu().getBlockEntity());

        UUID teamId = this.detectorBE.getTeamId();
        long detectorThreshold = this.detectorBE.getThreshold();

        boolean shouldUpdateDueToNulls = (this.teamId == null && teamId != null);

        boolean shouldUpdateDueToDifferences = (this.teamId != null && !this.teamId.equals(teamId)) ||
                (this.threshold != detectorThreshold);

        if (shouldUpdateDueToNulls || shouldUpdateDueToDifferences) {
            updateInformation();
        }

        // Check if input is valid
        this.textConfirmButton.setValid(isValidInput());
    }
}
