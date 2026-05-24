package com.ammonium.adminshop.network;

import com.ammonium.adminshop.AdminShop;
import com.ammonium.adminshop.blocks.interfaces.ShopMachine;
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

public class PacketUpdateRequest implements CustomPacketPayload {

    public static final Type<PacketUpdateRequest> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(AdminShop.MODID, "update_request"));
    public static final StreamCodec<FriendlyByteBuf, PacketUpdateRequest> STREAM_CODEC = StreamCodec.of(
            (buf, pkt) -> buf.writeBlockPos(pkt.pos),
            buf -> new PacketUpdateRequest(buf.readBlockPos())
    );

    private final BlockPos pos;

    public PacketUpdateRequest(BlockPos pos) {
        this.pos = pos;
    }

    public static void handle(PacketUpdateRequest packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            ServerLevel level = player.serverLevel();
            BlockEntity be = level.getBlockEntity(packet.pos);
            if (be instanceof ShopMachine autoShopMachine) {
                autoShopMachine.sendUpdates();
            }
            MoneyHelper.MoneyAccount ignored = MoneyHelper.get(level).getPlayerAccount(player);
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
