package com.ammonium.adminshop.network;

import com.ammonium.adminshop.AdminShop;
import com.ammonium.adminshop.blocks.ItemBuyerMachine;
import com.ammonium.adminshop.money.MoneyManager;
import com.ammonium.adminshop.shop.Shop;
import com.ammonium.adminshop.shop.ShopItem;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PacketSetBuyerTarget {
    private final BlockPos pos;
    private final ShopItem targetItem;

    public PacketSetBuyerTarget(BlockPos pos, ShopItem targetItem) {
        this.pos = pos;
        this.targetItem = targetItem;
    }

    public PacketSetBuyerTarget(FriendlyByteBuf buf) {
        this.pos = buf.readBlockPos();
        this.targetItem = Shop.get().getShopStockBuy().get(buf.readInt());
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBlockPos(this.pos);
        buf.writeInt(Shop.get().getShopStockBuy().indexOf(this.targetItem));
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier){
        NetworkEvent.Context ctx = supplier.get();
        ctx.enqueueWork(() -> {
            //Client side accessed here
            //Do NOT call client-only code though, since server needs to access this too

            // Change machine's account
            ServerPlayer player = ctx.getSender();

            if (player != null) {
                System.out.println("Setting buyer target for "+this.pos+" to "+this.targetItem.toString());
                // Get IBuyerBE
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
                if (!moneyManager.getBankAccount(buyerEntity.getAccount()).containsMember(player.getStringUUID())) {
                    AdminShop.LOGGER.error("Player does not have access to this machine's account");
                    return;
                }
                System.out.println("Saving machine account information.");
                // Apply changes to buyerEntity
                buyerEntity.setTargetShopItem(this.targetItem);
                // Handled inside setTargetShopItem() now
//                blockEntity.setChanged();
//                buyerEntity.sendUpdates();
            }
        });
        return true;
    }
}
