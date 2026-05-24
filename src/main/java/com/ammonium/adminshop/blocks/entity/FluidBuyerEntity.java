package com.ammonium.adminshop.blocks.entity;

import com.ammonium.adminshop.AdminShop;
import com.ammonium.adminshop.blocks.interfaces.FluidBuyerMachine;
import com.ammonium.adminshop.recipes.BuyFluidRecipe;
import com.ammonium.adminshop.recipes.RecipeManager;
import com.ammonium.adminshop.screen.FluidBuyerMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
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

import java.util.Optional;
import java.util.UUID;

public class FluidBuyerEntity extends FluidHandlerBlockEntity implements FluidBuyerMachine {
    private static final int TANK_CAPACITY = 64000;
    public static final int TICK_COOLDOWN = 20;

    private UUID teamId = null;
    private ResourceLocation recipeId = null;
    private int tickCounter = 0;  // unsynced
    private int tickProgress = 0; // synced

    public FluidBuyerEntity(BlockPos pWorldPosition, BlockState pBlockState) {
        super(ModBlockEntities.FLUID_BUYER.get(), pWorldPosition, pBlockState);
//        this.tank = new ExtractOnlyTank(TANK_CAPACITY, this::sendUpdates);
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
    public UUID getTeamId() {
        return teamId;
    }

    @Override
    public void setRecipe(ResourceLocation recipeId) {
        this.recipeId = recipeId;
    }

    @Override
    public Optional<BuyFluidRecipe> getRecipe(Level level) {
        return RecipeManager.getShopBuyFluidRecipe(level, recipeId).map(net.minecraft.world.item.crafting.RecipeHolder::value);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.adminshop.fluid_buyer");
    }

    @Override
    public void setChanged() {
        super.setChanged();
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int pContainerId, Inventory pInventory, Player pPlayer) {
        return new FluidBuyerMenu(pContainerId, pInventory, this);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, FluidBuyerEntity buyerBE) {
        // Ignore if not server side
        if (level.isClientSide) { return; }
        assert level instanceof ServerLevel;

        // Only run every 20 ticks
        int tickCounter = buyerBE.getTickCounter() + 1;
        if (tickCounter <= TICK_COOLDOWN) {
            buyerBE.setTickCounter(tickCounter);
            return;
        }
        buyerBE.setTickCounter(0);

        // Check for valid recipe
        BuyFluidRecipe recipe = buyerBE.getRecipe((ServerLevel) level).orElse(null);
        if (recipe == null) {
            AdminShop.LOGGER.info("[FluidBuyer] No recipe set (recipeId={})", buyerBE.recipeId);
            return;
        }
        boolean isValid = RecipeManager.checkForBuyFluidRecipe((ServerLevel) level, buyerBE, recipe);
        if (!isValid) {
            AdminShop.LOGGER.info("[FluidBuyer] Recipe invalid (teamId={}, price={}, permit={})", buyerBE.teamId, recipe.getPrice(), recipe.getPermit());
            return;
        }

        // Check for space
        net.neoforged.neoforge.fluids.capability.IFluidHandler handler = buyerBE.getTank();
        if (handler == null) {
            AdminShop.LOGGER.info("[FluidBuyer] No fluid handler");
            return;
        }
        FluidStack simulate = recipe.getFluid();
//        AdminShop.LOGGER.info("[FluidBuyer] Tank: {}mB / {}mB", handler.getFluidInTank(0).getAmount(), handler.getTankCapacity(0));
        int filled = handler.fill(simulate, IFluidHandler.FluidAction.SIMULATE);
//        if (filled != simulate.getAmount()) {
//            AdminShop.LOGGER.info("[FluidBuyer] Fill simulate: filled={}/{} fluid={} (tank full or invalid)", filled, simulate.getAmount(), simulate.getDescriptionId());
//        }
        if (filled == simulate.getAmount()) {

            // Buy the fluid
            FluidStack buy = recipe.buy((ServerLevel) level, buyerBE);
            assert buy != null && !buy.isEmpty();
            handler.fill(buy, IFluidHandler.FluidAction.EXECUTE);
            buyerBE.setChanged();
            buyerBE.sendUpdates();
            return;
        }
    }

//    @Nonnull
//    @Override
//    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @javax.annotation.Nullable Direction side) {
//        if (cap == ForgeCapabilities.FLUID_HANDLER) {
//            return lazyItemHandler.cast();
//        }
//        return super.getCapability(cap, side);
//    }

    @Override
    public void onLoad() {
        super.onLoad();
//        lazyItemHandler = LazyOptional.of(() -> itemHandler);
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
        if (this.recipeId != null) {
            tag.putString("recipe", this.recipeId.toString());
        }
        tag.putInt("tickProgress", this.tickProgress);
    }

    @Override
    public void loadAdditional(@NotNull CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        if (tag.contains("team")) {
            this.teamId = tag.getUUID("team");
        }
        if (tag.contains("recipe")) {
            this.recipeId = ResourceLocation.parse(tag.getString("recipe"));
        } else {
            this.recipeId = null;
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
