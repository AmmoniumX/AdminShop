package com.ammonium.adminshop.blocks.entity;

import com.ammonium.adminshop.AdminShop;
import com.ammonium.adminshop.blocks.FluidSellerMachine;
import com.ammonium.adminshop.recipes.RecipeManager;
import com.ammonium.adminshop.recipes.SellFluidRecipe;
import com.ammonium.adminshop.screen.FluidSellerMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
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
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.capability.FluidHandlerBlockEntity;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class FluidSellerBE extends FluidHandlerBlockEntity implements FluidSellerMachine {
    private String ownerUUID;
    private Pair<String, Integer> account;
    private int tickCounter = 0;
    private static final int TANK_CAPACITY = 64000;

    public FluidSellerBE(BlockPos pWorldPosition, BlockState pBlockState) {
        super(ModBlockEntities.FLUID_SELLER.get(), pWorldPosition, pBlockState);
//        this.tank = new InsertSellableOnlyTank(TANK_CAPACITY, this::sendUpdates);
        this.tank = new FluidTank(TANK_CAPACITY);
    }

    public void setOwnerUUID(String ownerUUID) {
        this.ownerUUID = ownerUUID;
        this.setChanged();
        this.sendUpdates();
    }

    public String getOwnerUUID() {
        return ownerUUID;
    }

    public void setAccount(Pair<String, Integer> account) {
        this.account = account;
        this.setChanged();
        this.sendUpdates();
    }

    public Pair<String, Integer> getAccountId() {
        return account;
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

    public static void tick(Level level, BlockPos pos, BlockState state, FluidSellerBE sellerBE) {
        // Ignore if not server side
        if (level.isClientSide) { return; }
        assert level instanceof ServerLevel;

        // Only run every 20 ticks
        sellerBE.tickCounter++;
        if (sellerBE.tickCounter <= 20) { return; }
        sellerBE.tickCounter = 0;

        // Check for valid recipe
        SellFluidRecipe recipe = RecipeManager.checkForSellFluidRecipe((ServerLevel) level, sellerBE).orElse(null);
        if (recipe == null) {
            AdminShop.LOGGER.debug("No recipe found for sellerBE");
            return;
        }

        // Buy the fluid
        IFluidHandler handler = sellerBE.getCapability(ForgeCapabilities.FLUID_HANDLER).orElse(null);
        if (handler == null) {
            AdminShop.LOGGER.debug("Fluid handler is null");
            return;
        }
        handler.drain(recipe.getFluid(), IFluidHandler.FluidAction.EXECUTE);
        recipe.sell((ServerLevel) level, sellerBE);
        return;
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

    @Override
    public void invalidateCaps()  {
        super.invalidateCaps();
//        lazyItemHandler.invalidate();
    }

    @Override
    public @NotNull CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
//        tag.put("inventory", this.itemHandler.serializeNBT());
        tank.writeToNBT(tag);
        if (this.ownerUUID != null) {
            tag.putString("ownerUUID", this.ownerUUID);
        }
        if (this.account != null) {
            tag.putString("accountUUID", this.account.getKey());
            tag.putInt("accountID", this.account.getValue());
        }
        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt) {
        super.onDataPacket(net, pkt);
        this.load(Objects.requireNonNull(pkt.getTag()));
    }
    public void sendUpdates() {
        if (this.level != null) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
        }
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        super.handleUpdateTag(tag);
//        this.itemHandler.deserializeNBT(tag.getCompound("inventory"));
        this.tank.readFromNBT(tag);
        if (tag.contains("ownerUUID")) {
            this.ownerUUID = tag.getString("ownerUUID");
        }
        if (tag.contains("accountUUID") && tag.contains("accountID")) {
            String accountUUID = tag.getString("accountUUID");
            int accountID = tag.getInt("accountID");
            this.account = Pair.of(accountUUID, accountID);
        }
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag) {
        super.saveAdditional(tag);
//        tank.writeToNBT(tag);
        if (this.ownerUUID != null) {
            tag.putString("ownerUUID", this.ownerUUID);
        }
        if (this.account != null) {
            tag.putString("accountUUID", this.account.getKey());
            tag.putInt("accountID", this.account.getValue());
        }
    }

    @Override
    public void load(@NotNull CompoundTag tag) {
        super.load(tag);
//        tank.readFromNBT(tag);
        if (tag.contains("ownerUUID")) {
            this.ownerUUID = tag.getString("ownerUUID");
        }
        if (tag.contains("accountUUID") && tag.contains("accountID")) {
            String accountUUID = tag.getString("accountUUID");
            int accountID = tag.getInt("accountID");
            this.account = Pair.of(accountUUID, accountID);
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
