package com.ammonium.adminshop.network;

import com.ammonium.adminshop.AdminShop;
import com.ammonium.adminshop.blocks.interfaces.ItemBuyerMachine;
import com.ammonium.adminshop.money.MoneyManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PacketSetItemBuyerRecipe {
    private final BlockPos pos;
    private final ResourceLocation recipeId;

    public PacketSetItemBuyerRecipe(BlockPos pos, ResourceLocation recipeId) {
        this.pos = pos;
        this.recipeId = recipeId;
    }

    public PacketSetItemBuyerRecipe(FriendlyByteBuf buf) {
        this.pos = buf.readBlockPos();
        this.recipeId = buf.readResourceLocation();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBlockPos(this.pos);
        buf.writeResourceLocation(this.recipeId);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier){
        NetworkEvent.Context ctx = supplier.get();
        ctx.enqueueWork(() -> {
            //Client side accessed here
            //Do NOT call client-only code though, since server needs to access this too

            // Change machine's account
            ServerPlayer player = ctx.getSender();

            if (player != null) {
                System.out.println("Setting buyer recipe for "+this.pos+" to "+this.recipeId);
                Level level = player.level;
                BlockEntity blockEntity = level.getBlockEntity(this.pos);
                if (!(blockEntity instanceof ItemBuyerMachine buyerEntity)) {
                    AdminShop.LOGGER.error("BlockEntity at pos is not BuyerMachine");
                    return;
                }
                // Check machine's owner is the same as player
//                if (!buyerEntity.getOwnerUUID().equals(player.getStringUUID())) {

                // Check if player has access to the machine's account
                MoneyManager moneyManager = MoneyManager.get(player.getLevel());
                if (!moneyManager.getBankAccount(buyerEntity.getAccountId()).containsMember(player.getStringUUID())) {
                    AdminShop.LOGGER.error("Player does not have access to this machine's account");
                    return;
                }
                // Apply changes to buyerEntity
                buyerEntity.setRecipe(this.recipeId);
            }
        });
        return true;
    }
}
