package com.ammonium.adminshop.blocks.entity;

import com.ammonium.adminshop.AdminShop;
import com.ammonium.adminshop.blocks.BasicDetector;
import com.ammonium.adminshop.blocks.interfaces.Detector;
import com.ammonium.adminshop.money.MoneyHelper;
import com.ammonium.adminshop.screen.AdvancedDetectorMenu;
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
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class AdvancedDetectorEntity extends BlockEntity implements Detector {
    private int tickCounter = 0;
    private @Nullable UUID teamId = null;
    private long threshold = 0;
    private int signal = 0;

    public void setSignal(int nsignal, BlockState state, Level level, BlockPos pos) {
        if (this.signal == nsignal) return;
        AdminShop.LOGGER.debug("Updating detector signal to "+nsignal);
        this.signal = nsignal;
        boolean newVal = signal > 0;
        BlockState currState = getBlockState();
        if (state.getValue(BasicDetector.LIT) != newVal && currState.getValue(BasicDetector.LIT) != newVal) {
            AdminShop.LOGGER.debug("Updating detector LIT value to "+newVal);
            level.setBlock(pos, state.setValue(BasicDetector.LIT, newVal), 3);
        }
        this.setChanged();
        this.sendUpdates();
        level.updateNeighborsAt(pos, getBlockState().getBlock());
    }

    public int getSignal() {
        return signal;
    }

    public AdvancedDetectorEntity(BlockPos pPos, BlockState pBlockState) {
        super(ModBlockEntities.ADVANCED_DETECTOR.get(), pPos, pBlockState);
    }

    @Override
    public void setTeamId(UUID teamId) {
        this.teamId = teamId;
    }

    @Override
    public @Nullable UUID getTeamId() {
        return teamId;
    }

    public void setThreshold(long threshold) {
        this.threshold = threshold;
        this.setChanged();
        this.sendUpdates();
    }

    public long getThreshold() {
        return threshold;
    }

    public static void tick(Level pLevel, BlockPos pPos, BlockState pState, AdvancedDetectorEntity pBlockEntity) {
        if(!pLevel.isClientSide) {
            pBlockEntity.tickCounter++;
            if (pBlockEntity.tickCounter > 20) {
                pBlockEntity.tickCounter = 0;
                assert pLevel instanceof ServerLevel;
                ServerLevel sLevel = (ServerLevel) pLevel;
                // Get account balance
                long balance = MoneyHelper.get(sLevel).getAccountById(pBlockEntity.getTeamId()).balance();
                // Get redstone level based on threshold, range from 0 to 15
                long threshold = pBlockEntity.getThreshold();
                int calculatedSignal = (int) Math.min(Math.floor((balance/(double) threshold)*15), 15);
                pBlockEntity.setSignal(calculatedSignal, pState, pLevel, pPos);
//                BlockState currentState = pLevel.getBlockState(pPos);
//                boolean newVal = balance > threshold;
//                if (pState.getValue(BasicDetector.LIT) != newVal && currentState.getValue(BasicDetector.LIT) != newVal) {
//                    AdminShop.LOGGER.debug("Updating detector level to "+newVal);
//                    pLevel.setBlock(pPos, pState.setValue(BasicDetector.LIT, newVal), 3);
//                    pBlockEntity.setChanged();
//                    pBlockEntity.sendUpdates();
//                }

            }
        }
    }

    @Override
    public void setChanged() {
        super.setChanged();
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
        tag.putLong("threshold", this.threshold);
    }

    @Override
    public void loadAdditional(@NotNull CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        if (tag.contains("team")) {
            this.teamId = tag.getUUID("team");
        }
        if (tag.contains("threshold")) {
            this.threshold = tag.getLong("threshold");
        }
    }


    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("screen.adminshop.adv_detector");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int pContainerId, Inventory pInventory, Player pPlayer) {
        return new AdvancedDetectorMenu(pContainerId, pInventory, this);
    }
}
