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
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.core.BlockPos;
import net.minecraft.client.input.MouseButtonEvent;
import org.joml.Matrix3x2fStack;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
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
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
import net.neoforged.neoforge.transfer.fluid.FluidStacksResourceHandler;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

public class FluidBuyerScreen extends AbstractContainerScreen<FluidBuyerMenu> {
    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath(AdminShop.MODID, "textures/gui/fluid_buyer.png");
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
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        Slot slot = this.getSlotUnderMouse();
        AtomicBoolean override = new AtomicBoolean(false);
        if (slot != null) {
            ItemStack itemStack = slot.getItem();
            if (!itemStack.isEmpty()) {
                AdminShop.LOGGER.debug("Clicked on item: {}", itemStack.getDisplayName().getString());
                IFluidHandlerItem fluidHandler = FluidUtil.getFluidHandler(itemStack).orElse(null);
                if (fluidHandler != null) {
                    FluidStack fluid = fluidHandler.getFluidInTank(0);
                    if (!fluid.isEmpty()) {
                        net.minecraft.world.item.crafting.RecipeHolder<BuyFluidRecipe> recipeHolder =
                                RecipeManager.isBuyFluidRecipe(Minecraft.getInstance().level, fluid).orElse(null);
                        if (recipeHolder == null) {
                            AdminShop.LOGGER.debug("Fluid not in buy recipes: {}", fluid.getHoverName().getString());
                        } else if (this.buyerEntity.isLockedRecipe() && !isCreativeClickingPlayer()) {
                            // Recipe is locked server-side; don't touch the client's view of the target fluid.
                            LocalPlayer player = Minecraft.getInstance().player;
                            assert player != null;
                            player.sendSystemMessage(Component.translatable("message.adminshop.recipe_locked"));
                            override.set(true);
                        } else if (this.buyerEntity.isLockedRecipe()) {
                            // Creative players can still configure a locked machine's recipe.
                            this.buyerEntity.forceSetRecipe(recipeHolder.id().identifier());
                            this.recipe = recipeHolder.value();
                            Messages.sendToServer(new PacketSetFluidBuyerRecipe(this.blockPos, recipeHolder.id().identifier()));
                            override.set(true);
                        } else if (ClientCache.hasPermit(recipeHolder.value().getPermit())) {
                            this.buyerEntity.setRecipe(recipeHolder.id().identifier());
                            this.recipe = recipeHolder.value();
                            Messages.sendToServer(new PacketSetFluidBuyerRecipe(this.blockPos, recipeHolder.id().identifier()));
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
        return (!override.get() && super.mouseClicked(event, doubleClick));
    }

    private static boolean isCreativeClickingPlayer() {
        LocalPlayer player = Minecraft.getInstance().player;
        return player != null && player.isCreative();
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTicks) {
        super.extractBackground(guiGraphics, mouseX, mouseY, partialTicks);
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, 0, 0, imageWidth, imageHeight, 256, 256);
        if (this.recipe != null) {
            renderFluid(guiGraphics, this.recipe.getFluid().getFluid(), x+104, y+24, 16, 16);
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
        Matrix3x2fStack poseStack = guiGraphics.pose();
        poseStack.pushMatrix();
        if (this.tankGauge == null) {
            AdminShop.LOGGER.debug("TankGauge is null!");
        }
        if (this.tankGauge != null && tankGauge.isMouseOn) {
            guiGraphics.setTooltipForNextFrame(font, tankGauge.getTooltipContent(),
                    Optional.empty(), mouseX,
                    mouseY);
        }
        poseStack.popMatrix();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(guiGraphics, mouseX, mouseY, delta);

        // Get data from BlockEntity
        this.buyerEntity = this.getMenu().getBlockEntity();

        UUID teamId = this.buyerEntity.getTeamId();
        BuyFluidRecipe recipe = this.buyerEntity.getRecipe(Minecraft.getInstance().level).orElse(null);
        FluidStacksResourceHandler buyerTank = this.buyerEntity.getTank();

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

    private void renderFluid(GuiGraphicsExtractor guiGraphics, Fluid fluid, int x, int y, int width, int height) {
        // Set fluid texture if null
        if (fluidTexture == null) {
            setFluidTexture(fluid);
        }
        // Render Fluid
        guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, fluidTexture, x, y, width, height,
                ARGB.colorFromFloat(fluidColorA, fluidColorR, fluidColorG, fluidColorB));
    }

    private void setFluidTexture(Fluid fluid) {
        // Get fluid texture
        FluidModel properties = Minecraft.getInstance().getModelManager().getFluidStateModelSet()
                .get(fluid.defaultFluidState());
        fluidTexture = properties.stillMaterial().sprite();
        int fcol = properties.fluidTintSource() != null ? properties.fluidTintSource().colorAsStack(new FluidStack(fluid, 1)) : -1;
        if (ARGB.alpha(fcol) == 0) { fcol = ARGB.opaque(fcol); }
        fluidColorR = ((fcol >> 16) & 0xFF) / 255.0F;
        fluidColorG = ((fcol >> 8) & 0xFF) / 255.0F;
        fluidColorB = (fcol & 0xFF) / 255.0F;
        fluidColorA = ((fcol >> 24) & 0xFF) / 255.0F;
    }
}
