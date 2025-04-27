package com.ammonium.adminshop.setup;

import com.ammonium.adminshop.AdminShop;
import com.ammonium.adminshop.network.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public class Messages {

    private static SimpleChannel INSTANCE;
    private static int packetId = 0;
    private static int id() {
        return packetId++;
    }

    public static void register(){
        SimpleChannel net = NetworkRegistry.ChannelBuilder
                .named(new ResourceLocation(AdminShop.MODID, "messages"))
                .networkProtocolVersion(() -> "1.0")
                .clientAcceptedVersions(s -> true)
                .serverAcceptedVersions(s -> true)
                .simpleChannel();
        INSTANCE = net;
        net.messageBuilder(PacketSyncMoneyToClient.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(PacketSyncMoneyToClient::new)
                .encoder(PacketSyncMoneyToClient::toBytes)
                .consumerMainThread(PacketSyncMoneyToClient::handle)
                .add();
        net.messageBuilder(PacketBuyRequest.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(PacketBuyRequest::new)
                .encoder(PacketBuyRequest::toBytes)
                .consumerMainThread(PacketBuyRequest::handle)
                .add();
        net.messageBuilder(PacketSellRequest.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(PacketSellRequest::new)
                .encoder(PacketSellRequest::toBytes)
                .consumerMainThread(PacketSellRequest::handle)
                .add();
//        net.messageBuilder(PacketSyncShopToClient.class, id(), NetworkDirection.PLAY_TO_CLIENT)
//                .decoder(PacketSyncShopToClient::new)
//                .encoder(PacketSyncShopToClient::toBytes)
//                .consumerMainThread(PacketSyncShopToClient::handle)
//                .add();
        net.messageBuilder(PacketMachineAccountChange.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(PacketMachineAccountChange::new)
                .encoder(PacketMachineAccountChange::toBytes)
                .consumerMainThread(PacketMachineAccountChange::handle)
                .add();
        net.messageBuilder(PacketChangeDefaultAccount.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(PacketChangeDefaultAccount::new)
                .encoder(PacketChangeDefaultAccount::toBytes)
                .consumerMainThread(PacketChangeDefaultAccount::handle)
                .add();
        net.messageBuilder(PacketSetItemBuyerRecipe.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(PacketSetItemBuyerRecipe::new)
                .encoder(PacketSetItemBuyerRecipe::toBytes)
                .consumerMainThread(PacketSetItemBuyerRecipe::handle)
                .add();
        net.messageBuilder(PacketSetFluidBuyerRecipe.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(PacketSetFluidBuyerRecipe::new)
                .encoder(PacketSetFluidBuyerRecipe::toBytes)
                .consumerMainThread(PacketSetFluidBuyerRecipe::handle)
                .add();
        net.messageBuilder(PacketAccountAddPermit.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(PacketAccountAddPermit::new)
                .encoder(PacketAccountAddPermit::toBytes)
                .consumerMainThread(PacketAccountAddPermit::handle)
                .add();
        net.messageBuilder(PacketAccountRemovePermit.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(PacketAccountRemovePermit::new)
                .encoder(PacketAccountRemovePermit::toBytes)
                .consumerMainThread(PacketAccountRemovePermit::handle)
                .add();
        net.messageBuilder(PacketUpdateRequest.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(PacketUpdateRequest::new)
                .encoder(PacketUpdateRequest::toBytes)
                .consumerMainThread(PacketUpdateRequest::handle)
                .add();
        net.messageBuilder(PacketSetDetectorThreshold.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(PacketSetDetectorThreshold::new)
                .encoder(PacketSetDetectorThreshold::toBytes)
                .consumerMainThread(PacketSetDetectorThreshold::handle)
                .add();
    }

    public static <MSG> void sendToServer(MSG message) {
        INSTANCE.sendToServer(message);
    }

    public static <MSG> void sendToPlayer(MSG message, ServerPlayer player) {
        INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), message);
    }
}
