package com.ammonium.adminshop.blocks.entity;

import com.ammonium.adminshop.AdminShop;
import com.ammonium.adminshop.blocks.interfaces.FluidSellerMachine;
import com.ammonium.adminshop.recipes.RecipeManager;
import com.ammonium.adminshop.recipes.SellFluidRecipe;
import com.ammonium.adminshop.screen.FluidSellerMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class FluidSellerEntity extends FluidHandlerBlockEntity implements FluidSellerMachine {
    public static final int TICK_COOLDOWN = 20;
    private static final int TANK_CAPACITY = 64000;

    private @Nullable UUID teamId = null;
    private int tickCounter = 0; // unsynced
    private int tickProgress = 0; // synced

    public FluidSellerEntity(BlockPos pWorldPosition, BlockState pBlockState) {
        super(ModBlockEntities.FLUID_SELLER.get(), pWorldPosition, pBlockState);
        this.tank = new FluidTank(TANK_CAPACITY);
    }

    public int getTickCounter() {
        return this.tickCounter;
    }

    public void setTickCounter(int pTickCounter) {
        if (this.tickCounter == pTickCounter) { return; }
        int oldProgress = (this.tickCounter * 8) / TICK_COOLDOWN;
        int newProgress = (pTickCounter * 8) / TICK_COOLDOWN;
        this.tickCounter = pTickCounter;
        this.tickProgress = newProgress;
        if (oldProgress != newProgress) {
            this.setChanged();
            this.sendUpdates();
        }
    }

    public int getProgress() {
        // GetProgress returns the progress of the seller in the range of 0-8
        return this.tickProgress;
    }

    @Override
    public void setTeamId(UUID teamId) {
        this.teamId = teamId;
        this.setChanged();
        this.sendUpdates();
    }

    @Override
    public @Nullable UUID getTeamId() {
        return teamId;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.adminshop.fluid_seller");
    }

    @Override
    public void setChanged() {
        super.setChanged();
    }

    public FluidTank getTank() {
        return this.tank;
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int pContainerId, Inventory pInventory, Player pPlayer) {
        return new FluidSellerMenu(pContainerId, pInventory, this);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, FluidSellerEntity sellerBE) {
        // Ignore if not server side
        if (level.isClientSide) { return; }
        assert level instanceof ServerLevel;

        // Only run every 20 ticks
        int tickCounter = sellerBE.getTickCounter() + 1;
        if (tickCounter <= TICK_COOLDOWN) {
            sellerBE.setTickCounter(tickCounter);
            return;
        }
        sellerBE.setTickCounter(0);

        // Check for valid recipe
        FluidStack tankContents = sellerBE.getTank().getFluid();
        AdminShop.LOGGER.info("[FluidSeller] Tick — teamId={}, tank={} x{}mB", sellerBE.teamId,
                tankContents.isEmpty() ? "empty" : tankContents.getDescriptionId(), tankContents.getAmount());
        net.minecraft.world.item.crafting.RecipeHolder<SellFluidRecipe> recipeHolder = RecipeManager.checkForSellFluidRecipe((ServerLevel) level, sellerBE).orElse(null);
        if (recipeHolder == null) {
            AdminShop.LOGGER.info("[FluidSeller] No matching recipe found");
            return;
        }
        SellFluidRecipe recipe = recipeHolder.value();
        AdminShop.LOGGER.info("[FluidSeller] Matched recipe: price={}, fluid={} x{}mB", recipe.getPrice(),
                recipe.getFluid().getDescriptionId(), recipe.getFluid().getAmount());

        // Sell the fluid
        @Nullable IFluidHandler handler = sellerBE.getTank();
        if (handler == null) {
            AdminShop.LOGGER.info("[FluidSeller] Fluid handler is null");
            return;
        }

        handler.drain(recipe.getFluid().copy(), IFluidHandler.FluidAction.EXECUTE);
        recipe.sell((ServerLevel) level, sellerBE);
        sellerBE.setChanged();
        sellerBE.sendUpdates();
    }

    @Override
    public void onLoad() {
        super.onLoad();
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    public void sendUpdates() {
        if (this.level != null) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
        }
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        if (this.teamId != null) {
            tag.putUUID("team", this.teamId);
        }
        tag.putInt("tickProgress", this.tickProgress);
    }

    @Override
    public void loadAdditional(@NotNull CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        if (tag.contains("team")) {
            this.teamId = tag.getUUID("team");
        }
        if (tag.contains("tickProgress")) {
            this.tickProgress = tag.getInt("tickProgress");
        }
    }

//    public void drops() {
//        SimpleContainer inventory = new SimpleContainer(itemHandler.getSlots());
//        for (int i = 0; i < itemHandler.getSlots(); i++) {
//            inventory.setItem(i, itemHandler.getStackInSlot(i));
//        }
//
//        Containers.dropContents(this.level, this.worldPosition, inventory);
//    }
}
