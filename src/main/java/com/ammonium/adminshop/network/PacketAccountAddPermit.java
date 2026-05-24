package com.ammonium.adminshop.network;

import com.ammonium.adminshop.AdminShop;
import com.ammonium.adminshop.money.MoneyHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.UUID;

public class PacketAccountAddPermit implements CustomPacketPayload {

    public static final Type<PacketAccountAddPermit> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(AdminShop.MODID, "account_add_permit"));
    public static final StreamCodec<FriendlyByteBuf, PacketAccountAddPermit> STREAM_CODEC = StreamCodec.of(
            (buf, pkt) -> {
                buf.writeUUID(pkt.teamId);
                buf.writeUtf(pkt.permit);
                buf.writeInt(pkt.slotIndex);
            },
            buf -> new PacketAccountAddPermit(buf.readUUID(), buf.readUtf(), buf.readInt())
    );

    private final String permit;
    private final int slotIndex;
    private final UUID teamId;

    public PacketAccountAddPermit(MoneyHelper.MoneyAccount account, String permit, int slotIndex) {
        this.teamId = account.teamId();
        this.permit = permit;
        this.slotIndex = slotIndex;
    }

    public PacketAccountAddPermit(UUID teamId, String permit, int slotIndex) {
        this.teamId = teamId;
        this.permit = permit;
        this.slotIndex = slotIndex;
    }

    public static void handle(PacketAccountAddPermit packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            AdminShop.LOGGER.info("Adding permit tier " + packet.permit + " to " + packet.teamId);
            ServerLevel level = player.serverLevel();
            MoneyHelper.get(level).addPermit(packet.teamId, packet.permit);
            player.playNotifySound(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.0f, 1.0f);
            MoneyHelper.MoneyAccount account = MoneyHelper.get(level).getAccountById(packet.teamId);
            assert account != null;
            player.sendSystemMessage(Component.translatable("message.adminshop.add_permit", packet.permit, account.name()));
            player.getInventory().removeItem(packet.slotIndex, 1);
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
