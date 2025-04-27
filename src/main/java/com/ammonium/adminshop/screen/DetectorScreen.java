package com.ammonium.adminshop.screen;

import com.ammonium.adminshop.AdminShop;
import com.ammonium.adminshop.blocks.interfaces.Detector;
import com.ammonium.adminshop.client.gui.ChangeAccountButton;
import com.ammonium.adminshop.client.gui.TextConfirmButton;
import com.ammonium.adminshop.money.BankAccount;
import com.ammonium.adminshop.money.ClientLocalData;
import com.ammonium.adminshop.network.MojangAPI;
import com.ammonium.adminshop.network.PacketMachineAccountChange;
import com.ammonium.adminshop.network.PacketSetDetectorThreshold;
import com.ammonium.adminshop.network.PacketUpdateRequest;
import com.ammonium.adminshop.setup.Messages;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

public abstract class DetectorScreen<T extends DetectorMenu<Q>, Q extends Detector> extends AbstractContainerScreen<T> {
    private final Class<Q> detectorClass;
    private final BlockPos blockPos;
    private Q detectorBE;
    private String ownerUUID;
    private Pair<String, Integer> account;
    private long threshold;
    private ChangeAccountButton changeAccountButton;
    private TextConfirmButton textConfirmButton;
    private EditBox thresholdInputBox;
    private final List<Pair<String, Integer>> usableAccounts = new ArrayList<>();

    private int usableAccountsIndex = -1; // -1 for unset
    private String username = "";

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

    private Pair<String, Integer> getAccountDetails() {
        if (usableAccountsIndex == -1 || usableAccountsIndex >= this.usableAccounts.size()) {
            AdminShop.LOGGER.error("Account isn't properly set!");
            return this.usableAccounts.get(0);
        }
        return this.usableAccounts.get(this.usableAccountsIndex);
    }

    private BankAccount getBankAccount() {
        return ClientLocalData.getAccountMap().get(getAccountDetails());
    }

    private void createChangeAccountButton(int x, int y) {
        if(changeAccountButton != null) {
            removeWidget(changeAccountButton);
        }
        changeAccountButton = new ChangeAccountButton(x+119, y+47, (b) -> {
            Player player = Minecraft.getInstance().player;
            assert player != null;
            // Check if player is the owner
            if (!player.getStringUUID().equals(ownerUUID)) {
                player.sendSystemMessage(Component.literal("You are not the owner of this machine!"));
                return;
            }
            // Change accounts
            changeAccounts();
            Minecraft.getInstance().player.sendSystemMessage(Component.literal("Changed account to "+
                    this.username+":"+ getAccountDetails().getValue()));
        });
        addRenderableWidget(changeAccountButton);
    }
    private void createThresholdInputBox(int x, int y) {
        int boxWidth = 121;
        int boxHeight = 12;
        this.thresholdInputBox = new EditBox(font, x+38, y+24, boxWidth, boxHeight, Component.literal(""));
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
        Minecraft.getInstance().player.sendSystemMessage(Component.literal("Set detector threshold to "+value));
    }
    private void createTextConfirmButton(int x, int y) {
        if(textConfirmButton != null) {
            removeWidget(textConfirmButton);
        }
        textConfirmButton = new TextConfirmButton(x+159, y+24, (b) -> {
            Player player = Minecraft.getInstance().player;
            assert player != null;
            // Check if player is the owner
            if (!player.getStringUUID().equals(ownerUUID)) {
                player.sendSystemMessage(Component.literal("You are not the owner of this machine!"));
                return;
            }
            // Set threshold
            setThreshold();
        });
        addRenderableWidget(textConfirmButton);
    }

    private void changeAccounts() {
        // Check if bankAccount was in usableAccountsIndex
        if (this.usableAccountsIndex == -1) {
            AdminShop.LOGGER.error("BankAccount is not in usableAccountsIndex");
            return;
        }
        // Refresh usable accounts
        Pair<String, Integer> bankAccount = usableAccounts.get(usableAccountsIndex);
        List<Pair<String, Integer>> localAccountData = new ArrayList<>();
        ClientLocalData.getUsableAccounts().forEach(account -> localAccountData.add(Pair.of(account.getOwner(),
                account.getId())));
        if (!this.usableAccounts.equals(localAccountData)) {
            this.usableAccounts.clear();
            this.usableAccounts.addAll(localAccountData);
        }
        // Change account, either by resetting to first (personal) account or moving to next sorted account
        if (!this.usableAccounts.contains(bankAccount)) {
            this.usableAccountsIndex = 0;
        } else {
            this.usableAccountsIndex = (this.usableAccounts.indexOf(bankAccount) + 1) % this.usableAccounts.size();
        }
        // Update username
        this.username = MojangAPI.getUsernameByUUID(this.usableAccounts.get(usableAccountsIndex).getKey());
        // Send change packet
        Messages.sendToServer(new PacketMachineAccountChange(this.ownerUUID, getAccountDetails().getKey(),
                getAccountDetails().getValue(), this.blockPos));
    }
    @Override
    protected void init() {
        super.init();
        int relX = (this.width - this.imageWidth) / 2;
        int relY = (this.height - this.imageHeight) / 2;
        // Fetch usable accounts
        this.usableAccounts.clear();
        ClientLocalData.getUsableAccounts().forEach(account -> this.usableAccounts.add(Pair.of(account.getOwner(),
                account.getId())));
        if (this.usableAccounts.size() < 1) {
            AdminShop.LOGGER.warn("No usable accounts found!");
        }
        this.usableAccountsIndex = 0;
        this.username = MojangAPI.getUsernameByUUID(getAccountDetails().getKey());
        createChangeAccountButton(relX, relY);
        createThresholdInputBox(relX, relY);
        createTextConfirmButton(relX, relY);

        // Request update from server
        Messages.sendToServer(new PacketUpdateRequest(this.blockPos));
    }
    private void updateInformation() {
        this.ownerUUID = this.detectorBE.getOwnerUUID();
        this.account = this.detectorBE.getAccountId();
        this.threshold = this.detectorBE.getThreshold();

        this.usableAccounts.clear();
        ClientLocalData.getUsableAccounts().forEach(account -> this.usableAccounts.add(Pair.of(account.getOwner(),
                account.getId())));
        Optional<Pair<String, Integer>> search = this.usableAccounts.stream().filter(baccount ->
                this.account.equals(Pair.of(baccount.getKey(), baccount.getValue()))).findAny();
        if (search.isEmpty()) {
            AdminShop.LOGGER.error("Player does not have access to this detector!");
            this.usableAccountsIndex = -1;
        } else {
            Pair<String, Integer> result = search.get();
            this.usableAccountsIndex = this.usableAccounts.indexOf(result);
        }
    }

    @Override
    protected void renderBg(PoseStack pPoseStack, float pPartialTicks, int pMouseX, int pMouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, getTexture());
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        this.blit(pPoseStack, x, y, 0, 0, imageWidth, imageHeight);
    }

    @Override
    protected void renderLabels(PoseStack pPoseStack, int pMouseX, int pMouseY) {
        super.renderLabels(pPoseStack, pMouseX, pMouseY);
        if (this.usableAccounts == null || this.usableAccountsIndex == -1 || this.usableAccountsIndex >=
                this.usableAccounts.size()) {
            return;
        }
        Pair<String, Integer> account = getAccountDetails();
        boolean accAvailable = this.usableAccountsIndex != -1 && ClientLocalData.accountAvailable(account.getKey(),
                account.getValue());
        int color = accAvailable ? 0xffffff : 0xff0000;
        drawString(pPoseStack, font, this.username+":"+ account.getValue(),
                7,48,color);
    }

    @Override
    public void render(PoseStack pPoseStack, int mouseX, int mouseY, float delta) {
        renderBackground(pPoseStack);
        super.render(pPoseStack, mouseX, mouseY, delta);
        renderTooltip(pPoseStack, mouseX, mouseY);

        // Get data from BlockEntity
        this.detectorBE = this.detectorClass.cast(this.getMenu().getBlockEntity());

        String detectorOwnerUUID = this.detectorBE.getOwnerUUID();
        Pair<String, Integer> detectorAccount = this.detectorBE.getAccountId();
        long detectorThreshold = this.detectorBE.getThreshold();

        boolean shouldUpdateDueToNulls = (this.ownerUUID == null && detectorOwnerUUID != null) ||
                (this.account == null && detectorAccount != null);

        boolean shouldUpdateDueToDifferences = (this.ownerUUID != null && !this.ownerUUID.equals(detectorOwnerUUID)) ||
                (this.account != null && !this.account.equals(detectorAccount)) ||
                (this.threshold != detectorThreshold);

        if (shouldUpdateDueToNulls || shouldUpdateDueToDifferences) {
            updateInformation();
        }

        // Check if input is valid
        this.textConfirmButton.setValid(isValidInput());
    }
}
