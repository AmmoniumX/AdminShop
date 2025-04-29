package com.ammonium.adminshop.network;

import com.ammonium.adminshop.blocks.interfaces.ShopMachine;
import com.ammonium.adminshop.money.MoneyHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PacketUpdateRequest {
    private final BlockPos pos;
    public PacketUpdateRequest(BlockPos pos) {
        this.pos = pos;
    }
    public PacketUpdateRequest(FriendlyByteBuf buf) {
        this.pos = buf.readBlockPos();
    }
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBlockPos(this.pos);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier){
        NetworkEvent.Context ctx = supplier.get();
        ctx.enqueueWork(() -> {
            //Client side accessed here
            //Do NOT call client-only code though, since server needs to access this too

            // Update machine's account
            ServerPlayer player = ctx.getSender();
            if (player != null) {
                ServerLevel level = player.getLevel();
                BlockEntity be = player.level.getBlockEntity(pos);
                if (be instanceof ShopMachine autoShopMachine) {
                    autoShopMachine.sendUpdates();
                }
                // Update player's client cache
                // Getting the account innately updates to the owners
                MoneyHelper.MoneyAccount ignored = MoneyHelper.get(level).getPlayerAccount(player);
            }

        });
        return true;
    }
}
