package com.ammonium.adminshop.blocks.entity;

import com.ammonium.adminshop.AdminShop;
import com.ammonium.adminshop.blocks.BasicDetector;
import com.ammonium.adminshop.blocks.Detector;
import com.ammonium.adminshop.money.MoneyManager;
import com.ammonium.adminshop.screen.AdvancedDetectorMenu;
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
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class AdvancedDetectorBE extends BlockEntity implements Detector {
    private int tickCounter = 0;
    private String ownerUUID;
    private Pair<String, Integer> account;
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

    public AdvancedDetectorBE(BlockPos pPos, BlockState pBlockState) {
        super(ModBlockEntities.ADVANCED_DETECTOR.get(), pPos, pBlockState);
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

    public void setThreshold(long threshold) {
        this.threshold = threshold;
        this.setChanged();
        this.sendUpdates();
    }

    public long getThreshold() {
        return threshold;
    }

    public static void tick(Level pLevel, BlockPos pPos, BlockState pState, AdvancedDetectorBE pBlockEntity) {
        if(!pLevel.isClientSide) {
            pBlockEntity.tickCounter++;
            if (pBlockEntity.tickCounter > 20) {
                pBlockEntity.tickCounter = 0;
                assert pLevel instanceof ServerLevel;
                ServerLevel sLevel = (ServerLevel) pLevel;
                // Get account balance
                MoneyManager moneyManager = MoneyManager.get(sLevel);
                long balance = moneyManager.getBalance(pBlockEntity.account.getKey(), pBlockEntity.account.getValue());
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

    @Override
    public @NotNull CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        if (this.ownerUUID != null) {
            tag.putString("ownerUUID", this.ownerUUID);
        }
        if (this.account != null) {
            tag.putString("accountUUID", this.account.getKey());
            tag.putInt("accountID", this.account.getValue());
        }
        tag.putLong("threshold", this.threshold);
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
        if (tag.contains("ownerUUID")) {
            this.ownerUUID = tag.getString("ownerUUID");
        }
        if (tag.contains("accountUUID") && tag.contains("accountID")) {
            String accountUUID = tag.getString("accountUUID");
            int accountID = tag.getInt("accountID");
            this.account = Pair.of(accountUUID, accountID);
        }
        if (tag.contains("threshold")) {
            this.threshold = tag.getLong("threshold");
        }
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag) {
        super.saveAdditional(tag);
        if (this.ownerUUID != null) {
            tag.putString("ownerUUID", this.ownerUUID);
        }
        if (this.account != null) {
            tag.putString("accountUUID", this.account.getKey());
            tag.putInt("accountID", this.account.getValue());
        }
        tag.putLong("threshold", this.threshold);
    }
    @Override
    public void load(@NotNull CompoundTag tag) {
        super.load(tag);
        if (tag.contains("ownerUUID")) {
            this.ownerUUID = tag.getString("ownerUUID");
        }
        if (tag.contains("accountUUID") && tag.contains("accountID")) {
            String accountUUID = tag.getString("accountUUID");
            int accountID = tag.getInt("accountID");
            this.account = Pair.of(accountUUID, accountID);
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
