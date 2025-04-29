package com.ammonium.adminshop.network;

import com.ammonium.adminshop.AdminShop;
import com.ammonium.adminshop.money.ClientCache;
import com.ammonium.adminshop.money.MoneyHelper;
import com.google.common.collect.ImmutableSet;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.UUID;
import java.util.function.Supplier;

public class PacketSyncMoneyToClient {

    private final UUID teamId;
    private final long balance;
    private final ImmutableSet<String> permits;

    public PacketSyncMoneyToClient(MoneyHelper.MoneyAccount account){
        this.teamId = account.teamId();
        this.balance = account.balance();
        this.permits = account.permits();
    }

    public PacketSyncMoneyToClient(UUID teamId, long balance, Collection<String> permits){
        this.teamId = teamId;
        this.balance = balance;
        this.permits = ImmutableSet.copyOf(permits);
    }

    public PacketSyncMoneyToClient(FriendlyByteBuf buf){
        this.teamId = buf.readUUID();
        this.balance = buf.readLong();
        this.permits = ImmutableSet.copyOf((Collection<? extends String>) buf.readCollection(LinkedHashSet::new, FriendlyByteBuf::readUtf));
    }

    public void toBytes(FriendlyByteBuf buf){
        buf.writeUUID(teamId);
        buf.writeLong(balance);
        buf.writeCollection(permits, FriendlyByteBuf::writeUtf);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier){
        NetworkEvent.Context ctx = supplier.get();
        ctx.enqueueWork(() -> {
            //Client side accessed here
            //Do NOT call client-only code though, since server needs to access this too
            AdminShop.LOGGER.debug("Syncing money to client: {} {} {}", teamId, balance, permits);
            ClientCache.setAccount(teamId, balance, permits);
        });
        return true;
    }
}
