package com.ammonium.adminshop.network;

import com.ammonium.adminshop.AdminShop;
import com.ammonium.adminshop.money.MoneyHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public class PacketAccountAddPermit {
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

    public PacketAccountAddPermit(FriendlyByteBuf buf) {
        this.teamId = buf.readUUID();
        this.permit = buf.readUtf();
        this.slotIndex = buf.readInt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(teamId);
        buf.writeUtf(permit);
        buf.writeInt(slotIndex);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context ctx = supplier.get();
        ctx.enqueueWork(() -> {
            //Client side accessed here
            //Do NOT call client-only code though, since server needs to access this too
            AdminShop.LOGGER.info("Adding permit tier "+permit+" to "+teamId);
            ServerPlayer player = ctx.getSender();
            assert player != null;
            ServerLevel level = player.getLevel();
            // Add permit
            MoneyHelper.get(level).addPermit(teamId, permit);
            player.playNotifySound(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.0f, 1.0f);
            player.sendSystemMessage(Component.literal("Adding permit tier "+permit+" to ").append(MoneyHelper.get(level).getAccountById(teamId).name()));
            // Remove item from user
            player.getInventory().removeItem(slotIndex, 1);
            // Sync money with affected clients
            AdminShop.LOGGER.debug("Syncing money with clients");
        });
        return true;
    }
}