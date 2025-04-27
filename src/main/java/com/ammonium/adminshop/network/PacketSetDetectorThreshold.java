package com.ammonium.adminshop.network;

import com.ammonium.adminshop.AdminShop;
import com.ammonium.adminshop.blocks.interfaces.Detector;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
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
                System.out.println("Setting detector threshold for "+this.pos+" to "+this.threshold);
                // Get IDetectorBE
                Level level = player.level;
                BlockEntity blockEntity = level.getBlockEntity(this.pos);
                if (!(blockEntity instanceof Detector detectorBE)) {
                    AdminShop.LOGGER.error("BlockEntity at pos is not Detector");
                    return;
                }
                // Check machine's owner is the same as player
                if (!detectorBE.getOwnerUUID().equals(player.getStringUUID())) {
                    AdminShop.LOGGER.error("Player is not the machine's owner");
                    return;
                }
                System.out.println("Saving detector information.");
                // Apply changes to detectorBE
                detectorBE.setThreshold(this.threshold);
                // Handled inside setThreshold()
//                blockEntity.setChanged();
//                detectorBE.sendUpdates();
            }
        });
        return true;
    }
}
