package com.ammonium.adminshop.screen;

import com.ammonium.adminshop.AdminShop;
import com.ammonium.adminshop.blocks.entity.FluidBuyerEntity;
import com.ammonium.adminshop.client.gui.TankGauge;
import com.ammonium.adminshop.money.ClientCache;
import com.ammonium.adminshop.money.MoneyHelper;
import com.ammonium.adminshop.network.PacketSetFluidBuyerRecipe;
import com.ammonium.adminshop.network.PacketUpdateRequest;
import com.ammonium.adminshop.recipes.BuyFluidRecipe;
import com.ammonium.adminshop.recipes.RecipeManager;
import com.ammonium.adminshop.setup.Messages;
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
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.templates.FluidTank;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

public class FluidBuyerScreen extends AbstractContainerScreen<FluidBuyerMenu> {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(AdminShop.MODID, "textures/gui/fluid_buyer.png");
    private final BlockPos blockPos;
    private FluidBuyerEntity buyerEntity;
    private UUID teamId = null;
    private BuyFluidRecipe recipe = null;
    private TextureAtlasSprite fluidTexture = null;
    private float fluidColorR, fluidColorG, fluidColorB, fluidColorA;
    private TankGauge tankGauge;

    public FluidBuyerScreen(FluidBuyerMenu pMenu, Inventory pPlayerInventory, Component pTitle, BlockPos blockPos) {
        super(pMenu, pPlayerInventory, pTitle);
        this.blockPos = blockPos;
    }

    @Override
    protected void init() {
        super.init();
        this.buyerEntity = this.getMenu().getBlockEntity();
        int relX = (this.width - this.imageWidth) / 2;
        int relY = (this.height - this.imageHeight) / 2;
        this.tankGauge = new TankGauge(this.buyerEntity.getTank(), relX+146, relY+10, 16, 50);
        addRenderableWidget(this.tankGauge);

        // Request update from server
        Messages.sendToServer(new PacketUpdateRequest(this.blockPos));
    }
    private void updateInformation(Level level) {
        this.teamId = this.buyerEntity.getTeamId();
        this.recipe = this.buyerEntity.getRecipe(level).orElse(null);
        this.tankGauge.setTank(this.buyerEntity.getTank());
        if (this.recipe != null) {setFluidTexture(this.recipe.getFluid().getFluid());}
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        Slot slot = this.getSlotUnderMouse();
        AtomicBoolean override = new AtomicBoolean(false);
        if (slot != null) {
            ItemStack itemStack = slot.getItem();
            if (!itemStack.isEmpty()) {
                // Get item clicked on
                AdminShop.LOGGER.debug("Clicked on item: {}", itemStack.getDisplayName().getString());
                // Check if item is container and has fluid
                // Check if item is fluid container
                itemStack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).ifPresent(fluidHandler -> {
                    FluidStack fluid = fluidHandler.getFluidInTank(0);
                    // Return if container is empty
                    if (fluid.isEmpty()) {
                        return;
                    }
                    // Check if fluid is in recipes
                    BuyFluidRecipe recipe = RecipeManager.isBuyFluidRecipe(Minecraft.getInstance().level, fluid).orElse(null);
                    // Return super if not in buy map
                    if (recipe == null) {
                        AdminShop.LOGGER.debug("Fluid not in buy recipes: {}", fluid.getDisplayName().getString());
                        return;
                    }
                    // Set buyer target
                    // Check if account has permit to buy item
                    if (ClientCache.hasPermit(recipe.getPermit())) {
                        this.buyerEntity.setRecipe(recipe.getId());
                        this.recipe = recipe;
                        Messages.sendToServer(new PacketSetFluidBuyerRecipe(this.blockPos, this.recipe.getId()));
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
    protected void renderBg(PoseStack poseStack, float partialTicks, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, TEXTURE);
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        this.blit(poseStack, x, y, 0, 0, imageWidth, imageHeight);
        if (this.recipe != null) {
            renderFluid(poseStack, this.recipe.getFluid().getFluid(), x+104, y+24, 16, 16);
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
        poseStack.pushPose();
        if (this.tankGauge == null) {
            AdminShop.LOGGER.debug("TankGauge is null!");
        }
        if (this.tankGauge != null && tankGauge.isMouseOn) {
            renderTooltip(poseStack, tankGauge.getTooltipContent(),
                    Optional.empty(), mouseX-(this.width - this.imageWidth)/2,
                    mouseY-(this.height - this.imageHeight)/2);
        }
        poseStack.popPose();
    }

    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float delta) {
        renderBackground(poseStack);
        super.render(poseStack, mouseX, mouseY, delta);
        renderTooltip(poseStack, mouseX, mouseY);

        // Get data from BlockEntity
        this.buyerEntity = this.getMenu().getBlockEntity();

        UUID teamId = this.buyerEntity.getTeamId();
        BuyFluidRecipe recipe = this.buyerEntity.getRecipe(Minecraft.getInstance().level).orElse(null);
        FluidTank buyerTank = this.buyerEntity.getTank();

        boolean shouldUpdateDueToNulls =
                (this.teamId == null && teamId != null) ||
                (this.recipe == null && recipe != null) ||
                (this.tankGauge.getTank() == null && buyerTank != null);

        boolean shouldUpdateDueToDifferences =
                (this.teamId != null && !this.teamId.equals(teamId)) ||
                (this.recipe != recipe) ||
                (!this.tankGauge.getTank().equals(buyerTank));

        if (shouldUpdateDueToNulls || shouldUpdateDueToDifferences) {
            updateInformation(Minecraft.getInstance().level);
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
