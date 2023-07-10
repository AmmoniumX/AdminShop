package com.ammonium.adminshop.network;

import com.ammonium.adminshop.AdminShop;
import com.ammonium.adminshop.money.ClientLocalData;
import com.ammonium.adminshop.setup.Messages;
import com.ammonium.adminshop.shop.Shop;
import com.ammonium.adminshop.shop.ShopItem;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.commons.lang3.tuple.Pair;

import java.util.function.Supplier;

public class PacketBuyerInfo {

    private final String machineOwnerUUID;
    private final String accOwnerUUID;
    private final int accID;
    private final BlockPos pos;
    private final boolean hasTarget;
    private final ResourceLocation target;

    public PacketBuyerInfo(String machineOwnerUUID, String accOwnerUUID, int accID, BlockPos pos) {
        this.machineOwnerUUID = machineOwnerUUID;
        this.accOwnerUUID = accOwnerUUID;
        this.accID = accID;
        this.pos = pos;
        this.hasTarget = false;
        this.target = null;
    }
    public PacketBuyerInfo(String machineOwnerUUID, String accOwnerUUID, int accID, BlockPos pos, ResourceLocation target) {
        this.machineOwnerUUID = machineOwnerUUID;
        this.accOwnerUUID = accOwnerUUID;
        this.accID = accID;
        this.pos = pos;
        this.hasTarget = true;
        this.target = target;
    }

    public PacketBuyerInfo(FriendlyByteBuf buf) {
        this.machineOwnerUUID = buf.readUtf();
        this.accOwnerUUID = buf.readUtf();
        this.accID = buf.readInt();
        this.pos = buf.readBlockPos();
        this.hasTarget = buf.readBoolean();
        if (hasTarget) {
            this.target = buf.readResourceLocation();
        } else {
            this.target = null;
        }
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUtf(this.machineOwnerUUID);
        buf.writeUtf(this.accOwnerUUID);
        buf.writeInt(this.accID);
        buf.writeBlockPos(this.pos);
        buf.writeBoolean(this.hasTarget);
        if (this.hasTarget) {
            buf.writeResourceLocation(this.target);
        }
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier){
        NetworkEvent.Context ctx = supplier.get();
        ctx.enqueueWork(() -> {
            //Client side accessed here
            //Do NOT call client-only code though, since server needs to access this too
//            Player player = Minecraft.getInstance().player;
                System.out.println("Receiving buyer info for "+this.pos);
                // Update ClientLocalData with received data
                ClientLocalData.addMachineAccount(this.pos, Pair.of(this.accOwnerUUID, this.accID));
                ClientLocalData.addMachineOwner(this.pos, this.machineOwnerUUID);
                // Update item target if present
                if (this.hasTarget) {
                    System.out.println("Adding target");
                    // Get item from itemName
                    Item item = ForgeRegistries.ITEMS.getValue(this.target);
                    // Check if item is in buyMap;
                    if (!Shop.get().getShopBuyMap().containsKey(item)) {
                        AdminShop.LOGGER.error("Item is not in BuyMap");
                        return;
                    }
                    ShopItem shopItem = Shop.get().getShopBuyMap().get(item);
                    ClientLocalData.addBuyerTarget(pos, shopItem);
                }
                // Send open menu packet
//                Level level = player.level;
//                BlockEntity blockEntity = level.getBlockEntity(this.pos);
//                if (!(blockEntity instanceof IBuyerBE)) {
//                    AdminShop.LOGGER.error("BlockEntity is not Buyer");
//                    return;
//                }
                // Request to open menu
                Messages.sendToServer(new PacketOpenMenu(this.pos));
        });
        return true;
    }

}