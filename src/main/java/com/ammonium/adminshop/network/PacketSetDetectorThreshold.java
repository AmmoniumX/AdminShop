package com.ammonium.adminshop.network;

import com.ammonium.adminshop.AdminShop;
import com.ammonium.adminshop.blocks.interfaces.Detector;
import com.ammonium.adminshop.money.MoneyHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PacketSetDetectorThreshold {
    private final BlockPos pos;
    private final long threshold;

    public PacketSetDetectorThreshold(BlockPos pos, long threshold) {
        this.pos = pos;
        this.threshold = threshold;
    }

    public PacketSetDetectorThreshold(FriendlyByteBuf buf) {
        this.pos = buf.readBlockPos();
        this.threshold = buf.readLong();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBlockPos(this.pos);
        buf.writeLong(this.threshold);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier){
        NetworkEvent.Context ctx = supplier.get();
        ctx.enqueueWork(() -> {
            //Client side accessed here
            //Do NOT call client-only code though, since server needs to access this too

            // Change machine's account
            ServerPlayer player = ctx.getSender();
            if (player != null) {
                AdminShop.LOGGER.debug("Setting detector threshold for {} to {}", this.pos, this.threshold);
                ServerLevel level = player.getLevel();
                BlockEntity blockEntity = level.getBlockEntity(this.pos);
                if (!(blockEntity instanceof Detector detectorBE)) {
                    AdminShop.LOGGER.error("BlockEntity at pos is not Detector");
                    return;
                }
                // Check if player has access to the machine
                if (!MoneyHelper.get(level).isMemberOfTeam(detectorBE.getTeamId(), player)) {
                    AdminShop.LOGGER.error("Player does not have access to the machine");
                    return;
                }
                System.out.println("Saving detector information.");
                // Apply changes to detectorBE
                detectorBE.setThreshold(this.threshold);
            }
        });
        return true;
    }
}
