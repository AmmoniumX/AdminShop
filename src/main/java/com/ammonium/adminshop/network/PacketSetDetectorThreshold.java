package com.ammonium.adminshop.network;

import com.ammonium.adminshop.AdminShop;
import com.ammonium.adminshop.blocks.interfaces.Detector;
import com.ammonium.adminshop.money.MoneyHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class PacketSetDetectorThreshold implements CustomPacketPayload {

    public static final Type<PacketSetDetectorThreshold> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(AdminShop.MODID, "set_detector_threshold"));
    public static final StreamCodec<FriendlyByteBuf, PacketSetDetectorThreshold> STREAM_CODEC = StreamCodec.of(
            (buf, pkt) -> {
                buf.writeBlockPos(pkt.pos);
                buf.writeLong(pkt.threshold);
            },
            buf -> new PacketSetDetectorThreshold(buf.readBlockPos(), buf.readLong())
    );

    private final BlockPos pos;
    private final long threshold;

    public PacketSetDetectorThreshold(BlockPos pos, long threshold) {
        this.pos = pos;
        this.threshold = threshold;
    }

    public static void handle(PacketSetDetectorThreshold packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            AdminShop.LOGGER.debug("Setting detector threshold for {} to {}", packet.pos, packet.threshold);
            ServerLevel level = player.serverLevel();
            BlockEntity blockEntity = level.getBlockEntity(packet.pos);
            if (!(blockEntity instanceof Detector detectorBE)) {
                AdminShop.LOGGER.error("BlockEntity at pos is not Detector");
                return;
            }
            if (!MoneyHelper.get(level).isMemberOfTeam(detectorBE.getTeamId(), player)) {
                AdminShop.LOGGER.error("Player does not have access to the machine");
                return;
            }
            detectorBE.setThreshold(packet.threshold);
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
