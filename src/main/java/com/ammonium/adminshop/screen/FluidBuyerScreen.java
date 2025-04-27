package com.ammonium.adminshop.screen;

import com.ammonium.adminshop.AdminShop;
import com.ammonium.adminshop.blocks.entity.FluidBuyerBE;
import com.ammonium.adminshop.client.gui.ChangeAccountButton;
import com.ammonium.adminshop.client.gui.TankGauge;
import com.ammonium.adminshop.money.BankAccount;
import com.ammonium.adminshop.money.ClientLocalData;
import com.ammonium.adminshop.network.MojangAPI;
import com.ammonium.adminshop.network.PacketMachineAccountChange;
import com.ammonium.adminshop.network.PacketSetItemBuyerRecipe;
import com.ammonium.adminshop.network.PacketUpdateRequest;
import com.ammonium.adminshop.setup.Messages;
import com.ammonium.adminshop.shop.Shop;
import com.ammonium.adminshop.shop.ShopItem;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

public class FluidBuyerScreen extends AbstractContainerScreen<FluidBuyerMenu> {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(AdminShop.MODID, "textures/gui/fluid_buyer.png");
    private final BlockPos blockPos;
    private FluidBuyerBE buyerEntity;
    private String ownerUUID;
    private Pair<String, Integer> account;
    private ShopItem shopTarget = null;
    private ChangeAccountButton changeAccountButton;
    private final List<Pair<String, Integer>> usableAccounts = new ArrayList<>();

    private int usableAccountsIndex = -1; // -1 for unset
    private TextureAtlasSprite fluidTexture = null;
    private float fluidColorR, fluidColorG, fluidColorB, fluidColorA;
    private TankGauge tankGauge;
    private String username = "";

    public FluidBuyerScreen(FluidBuyerMenu pMenu, Inventory pPlayerInventory, Component pTitle, BlockPos blockPos) {
        super(pMenu, pPlayerInventory, pTitle);
        this.blockPos = blockPos;
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
        changeAccountButton = new ChangeAccountButton(x+119, y+62, (b) -> {
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
        // Send change package
//        System.out.println("Registering account change with server...");
        Messages.sendToServer(new PacketMachineAccountChange(this.ownerUUID, getAccountDetails().getKey(),
                getAccountDetails().getValue(), this.blockPos));
    }
    @Override
    protected void init() {
        super.init();
        this.buyerEntity = this.getMenu().getBlockEntity();
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
        this.tankGauge = new TankGauge(this.buyerEntity.getTank(), relX+146, relY+10, 16, 50);
        addRenderableWidget(this.tankGauge);

        // Request update from server
        Messages.sendToServer(new PacketUpdateRequest(this.blockPos));
    }
    private void updateInformation() {
        this.ownerUUID = this.buyerEntity.getOwnerUUID();
        this.account = this.buyerEntity.getAccountId();
        this.shopTarget = this.buyerEntity.getTargetShopItem();
        this.tankGauge.setTank(this.buyerEntity.getTank());
        if (this.shopTarget != null) {setFluidTexture(this.shopTarget.getFluid().getFluid());}

        this.usableAccounts.clear();
        ClientLocalData.getUsableAccounts().forEach(account -> this.usableAccounts.add(Pair.of(account.getOwner(),
                account.getId())));
        Optional<Pair<String, Integer>> search = this.usableAccounts.stream().filter(baccount ->
                this.account.equals(Pair.of(baccount.getKey(), baccount.getValue()))).findAny();
        if (search.isEmpty()) {
            AdminShop.LOGGER.error("Player does not have access to this seller!");
            this.usableAccountsIndex = -1;
        } else {
            Pair<String, Integer> result = search.get();
            this.usableAccountsIndex = this.usableAccounts.indexOf(result);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        Slot slot = this.getSlotUnderMouse();
        AtomicBoolean override = new AtomicBoolean(false);
        if (slot != null) {
            ItemStack itemStack = slot.getItem();
            if (!itemStack.isEmpty()) {
                // Get item clicked on
                StringBuilder debugMessage = new StringBuilder("Clicked on ItemStack: ");
                debugMessage.append(itemStack.getDisplayName().getString()).append(", ").append(itemStack.getCount()).append(", ");
                if (itemStack.getTag() != null) { debugMessage.append(itemStack.getTag()); }
                AdminShop.LOGGER.debug(debugMessage.toString());
                // Check if item is container and has fluid
                // Check if item is fluid container
                itemStack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).ifPresent(fluidHandler -> {
                    boolean isShopItem = false;
                    ShopItem shopItem = null;
                    FluidStack fluidStack = fluidHandler.getFluidInTank(0);
                    // Return if container is empty
                    if (fluidStack.isEmpty()) {
                        return;
                    }
                    Fluid fluid = fluidHandler.getFluidInTank(0).getFluid();
                    // Check if fluid is in fluid shop map
                    if (Shop.get().hasBuyShopFluid(fluid)) {
                        AdminShop.LOGGER.debug("ShopItem: " + fluidStack.getDisplayName().getString());
                        isShopItem = true;
                        shopItem = Shop.get().getBuyShopFluid(fluid);
                    }
                    // Return super if not in buy map
                    if (!isShopItem || shopItem == null) {
                        AdminShop.LOGGER.debug("Fluid not in buy map: " + fluidStack.getDisplayName().getString());
                        return;
                    }
                    // Set buyer target
                    // Check if account has permit to buy item
                    if (getBankAccount().hasPermit(shopItem.getPermitTier())) {
                        this.buyerEntity.setTargetShopItem(this.shopTarget);
                        this.shopTarget = shopItem;
                        Messages.sendToServer(new PacketSetItemBuyerRecipe(this.blockPos, this.shopTarget));
                        override.set(true);
                    } else {
                        LocalPlayer player = Minecraft.getInstance().player;
                        assert player != null;
                        player.sendSystemMessage(Component.literal("You haven't unlocked that yet!"));
                    }
                });
            }
        }
        return (!override.get() && super.mouseClicked(mouseX, mouseY, button));
    }

    @Override
    protected void renderBg(PoseStack pPoseStack, float pPartialTicks, int pMouseX, int pMouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, TEXTURE);
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        this.blit(pPoseStack, x, y, 0, 0, imageWidth, imageHeight);
        if (this.shopTarget != null) {
            renderFluid(pPoseStack, this.shopTarget.getFluid().getFluid(), x+104, y+24, 16, 16);
        }
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
                7,62,color);
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
        this.buyerEntity = this.getMenu().getBlockEntity();

        String buyerOwnerUUID = this.buyerEntity.getOwnerUUID();
        Pair<String, Integer> buyerAccount = this.buyerEntity.getAccountId();
        ShopItem buyerShopTarget = this.buyerEntity.getTargetShopItem();
        FluidTank buyerTank = this.buyerEntity.getTank();

//        AdminShop.LOGGER.debug("Buyer tank from screen render(): "+buyerTank.getFluid().getAmount()+"mb "+buyerTank.getFluid().getDisplayName().getString());
//        AdminShop.LOGGER.debug("Tanks are equal: "+this.tankGauge.getTank().equals(buyerTank));

        boolean shouldUpdateDueToNulls =
                (this.ownerUUID == null && buyerOwnerUUID != null) ||
                (this.account == null && buyerAccount != null) ||
                (this.shopTarget == null && buyerShopTarget != null) ||
                (this.tankGauge.getTank() == null && buyerTank != null);

        boolean shouldUpdateDueToDifferences =
                (this.ownerUUID != null && !this.ownerUUID.equals(buyerOwnerUUID)) ||
                (this.account != null && !this.account.equals(buyerAccount)) ||
                (this.shopTarget != buyerShopTarget) ||
                (!this.tankGauge.getTank().equals(buyerTank));

        if (shouldUpdateDueToNulls || shouldUpdateDueToDifferences) {
            updateInformation();
        }
    }

    private void renderFluid(PoseStack matrix, Fluid fluid, int x, int y, int width, int height) {
        // Set fluid texture if null
        if (fluidTexture == null) {
            setFluidTexture(fluid);
        }
        // Render Fluid
        RenderSystem.enableBlend();
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, fluidTexture.atlas().location());
        RenderSystem.setShaderColor(fluidColorR, fluidColorG, fluidColorB, fluidColorA);
        blit(matrix, x, y,0, width, height, fluidTexture);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.disableBlend();
    }

    private void setFluidTexture(Fluid fluid) {
        // Get fluid texture
        Function<ResourceLocation, TextureAtlasSprite> spriteAtlas = Minecraft.getInstance()
                .getTextureAtlas(InventoryMenu.BLOCK_ATLAS);
        IClientFluidTypeExtensions properties = IClientFluidTypeExtensions.of(fluid);
        ResourceLocation resource = properties.getStillTexture();
        fluidTexture = spriteAtlas.apply(resource);
        int fcol = properties.getTintColor();
        fluidColorR = ((fcol >> 16) & 0xFF) / 255.0F;
        fluidColorG = ((fcol >> 8) & 0xFF) / 255.0F;
        fluidColorB = (fcol & 0xFF) / 255.0F;
        fluidColorA = ((fcol >> 24) & 0xFF) / 255.0F;
    }
}
