package com.ammonium.adminshop.network;

import com.ammonium.adminshop.AdminShop;
import com.ammonium.adminshop.money.ClientCache;
import com.ammonium.adminshop.money.MoneyHelper;
import com.google.common.collect.ImmutableSet;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.UUID;

public class PacketSyncMoneyToClient implements CustomPacketPayload {

    public static final Type<PacketSyncMoneyToClient> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(AdminShop.MODID, "sync_money_to_client"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PacketSyncMoneyToClient> STREAM_CODEC = StreamCodec.of(
            (buf, pkt) -> {
                buf.writeUUID(pkt.teamId);
                buf.writeLong(pkt.balance);
                buf.writeVarInt(pkt.permits.size());
                pkt.permits.forEach(buf::writeUtf);
            },
            buf -> {
                UUID teamId = buf.readUUID();
                long balance = buf.readLong();
                int size = buf.readVarInt();
                ImmutableSet.Builder<String> permits = ImmutableSet.builder();
                for (int i = 0; i < size; i++) { permits.add(buf.readUtf()); }
                return new PacketSyncMoneyToClient(teamId, balance, permits.build());
            }
    );

    private final UUID teamId;
    private final long balance;
    private final ImmutableSet<String> permits;

    public PacketSyncMoneyToClient(MoneyHelper.MoneyAccount account) {
        this.teamId = account.teamId();
        this.balance = account.balance();
        this.permits = account.permits();
    }

    public PacketSyncMoneyToClient(UUID teamId, long balance, Collection<String> permits) {
        this.teamId = teamId;
        this.balance = balance;
        this.permits = ImmutableSet.copyOf(permits);
    }

    public static void handle(PacketSyncMoneyToClient packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            AdminShop.LOGGER.debug("Syncing money to client: {} {} {}", packet.teamId, packet.balance, packet.permits);
            ClientCache.setAccount(packet.teamId, packet.balance, packet.permits);
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
