package com.ammonium.adminshop.screen;

import com.ammonium.adminshop.AdminShop;
import com.ammonium.adminshop.blocks.entity.FluidBuyerEntity;
import com.ammonium.adminshop.client.gui.ProgressBar;
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
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

public class FluidBuyerScreen extends AbstractContainerScreen<FluidBuyerMenu> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(AdminShop.MODID, "textures/gui/fluid_buyer.png");
    private final BlockPos blockPos;
    private FluidBuyerEntity buyerEntity;
    private UUID teamId = null;
    private BuyFluidRecipe recipe = null;
    private TextureAtlasSprite fluidTexture = null;
    private int fluidTextureId = -1; // Texture ID for the fluid texture
    private float fluidColorR, fluidColorG, fluidColorB, fluidColorA;
    private TankGauge tankGauge;
    private ProgressBar progressBar;

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
        this.progressBar = new ProgressBar(relX + 79, relY + 25);
        addRenderableWidget(this.progressBar);

        // Request update from server
        Messages.sendToServer(new PacketUpdateRequest(this.blockPos));
    }
    private void updateInformation(Level level) {
        this.teamId = this.buyerEntity.getTeamId();
        this.recipe = this.buyerEntity.getRecipe(level).orElse(null);
        this.tankGauge.setTank(this.buyerEntity.getTank());
        if (this.recipe != null) {setFluidTexture(this.recipe.getFluid().getFluid());}
        this.progressBar.setProgress(this.buyerEntity.getProgress());
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        Slot slot = this.getSlotUnderMouse();
        AtomicBoolean override = new AtomicBoolean(false);
        if (slot != null) {
            ItemStack itemStack = slot.getItem();
            if (!itemStack.isEmpty()) {
                AdminShop.LOGGER.debug("Clicked on item: {}", itemStack.getDisplayName().getString());
                IFluidHandlerItem fluidHandler = itemStack.getCapability(Capabilities.FluidHandler.ITEM);
                if (fluidHandler != null) {
                    FluidStack fluid = fluidHandler.getFluidInTank(0);
                    if (!fluid.isEmpty()) {
                        net.minecraft.world.item.crafting.RecipeHolder<BuyFluidRecipe> recipeHolder =
                                RecipeManager.isBuyFluidRecipe(Minecraft.getInstance().level, fluid).orElse(null);
                        if (recipeHolder == null) {
                            AdminShop.LOGGER.debug("Fluid not in buy recipes: {}", fluid.getDisplayName().getString());
                        } else if (this.buyerEntity.isLockedRecipe()) {
                            // Recipe is locked server-side; don't touch the client's view of the target fluid.
                            LocalPlayer player = Minecraft.getInstance().player;
                            assert player != null;
                            player.sendSystemMessage(Component.translatable("message.adminshop.recipe_locked"));
                            override.set(true);
                        } else if (ClientCache.hasPermit(recipeHolder.value().getPermit())) {
                            this.buyerEntity.setRecipe(recipeHolder.id());
                            this.recipe = recipeHolder.value();
                            Messages.sendToServer(new PacketSetFluidBuyerRecipe(this.blockPos, recipeHolder.id()));
                            override.set(true);
                        } else {
                            LocalPlayer player = Minecraft.getInstance().player;
                            assert player != null;
                            player.sendSystemMessage(Component.translatable("gui.adminshop.no_permit"));
                        }
                    }
                }
            }
        }
        return (!override.get() && super.mouseClicked(mouseX, mouseY, button));
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTicks, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, TEXTURE);
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        guiGraphics.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight);
        if (this.recipe != null) {
            renderFluid(guiGraphics, this.recipe.getFluid().getFluid(), x+104, y+24, 16, 16);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);
        Component name = Component.translatable("gui.adminshop.no_account");
        boolean accAvailable = false;
        MoneyHelper.MoneyAccount account = ClientCache.getAccount();
        if (account != null) {
            name = account.name();
            accAvailable = true;
        }
        int color = accAvailable ? 0xffffff : 0xff0000;
        guiGraphics.drawString(font, name.getString(), 7,62,color);
        PoseStack poseStack = guiGraphics.pose();
        poseStack.pushPose();
        if (this.tankGauge == null) {
            AdminShop.LOGGER.debug("TankGauge is null!");
        }
        if (this.tankGauge != null && tankGauge.isMouseOn) {
            guiGraphics.renderTooltip(font, tankGauge.getTooltipContent(),
                    Optional.empty(), mouseX-(this.width - this.imageWidth)/2,
                    mouseY-(this.height - this.imageHeight)/2);
        }
        poseStack.popPose();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        renderBackground(guiGraphics, mouseX, mouseY, delta);
        super.render(guiGraphics, mouseX, mouseY, delta);
        renderTooltip(guiGraphics, mouseX, mouseY);

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
                (!this.tankGauge.getTank().equals(buyerTank)) ||
                (this.buyerEntity.getProgress() != this.progressBar.getProgress());

        if (shouldUpdateDueToNulls || shouldUpdateDueToDifferences) {
            updateInformation(Minecraft.getInstance().level);
        }
    }

    private void renderFluid(GuiGraphics guiGraphics, Fluid fluid, int x, int y, int width, int height) {
        // Set fluid texture if null
        if (fluidTexture == null) {
            setFluidTexture(fluid);
        }
        // Render Fluid
        RenderSystem.bindTexture(fluidTextureId);
        RenderSystem.setShaderColor(fluidColorR, fluidColorG, fluidColorB, fluidColorA);
        RenderSystem.setShaderTexture(0,
                fluidTexture.atlasLocation());
        guiGraphics.blit(x, y,0, width, height, fluidTexture);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
    }

    private void setFluidTexture(Fluid fluid) {
        // Get fluid texture
        Function<ResourceLocation, TextureAtlasSprite> spriteAtlas = Minecraft.getInstance()
                .getTextureAtlas(InventoryMenu.BLOCK_ATLAS);
        IClientFluidTypeExtensions properties = IClientFluidTypeExtensions.of(fluid);
        ResourceLocation resource = properties.getStillTexture();
        fluidTexture = spriteAtlas.apply(resource);
        TextureManager manager = Minecraft.getInstance().getTextureManager();
        AbstractTexture abstractTexture = manager.getTexture(InventoryMenu.BLOCK_ATLAS);
        TextureAtlas atlas = null;
        if (abstractTexture instanceof TextureAtlas) {
            atlas = (TextureAtlas) abstractTexture;
        }
        assert atlas != null;
        fluidTextureId = atlas.getId();
        int fcol = properties.getTintColor();
        fluidColorR = ((fcol >> 16) & 0xFF) / 255.0F;
        fluidColorG = ((fcol >> 8) & 0xFF) / 255.0F;
        fluidColorB = (fcol & 0xFF) / 255.0F;
        fluidColorA = ((fcol >> 24) & 0xFF) / 255.0F;
    }
}
