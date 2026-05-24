package com.ammonium.adminshop.setup;

import com.ammonium.adminshop.network.*;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public class Messages {

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToClient(
                PacketSyncMoneyToClient.TYPE,
                PacketSyncMoneyToClient.STREAM_CODEC,
                PacketSyncMoneyToClient::handle);
        registrar.playToServer(
                PacketBuyRequest.TYPE,
                PacketBuyRequest.STREAM_CODEC,
                PacketBuyRequest::handle);
        registrar.playToServer(
                PacketSellRequest.TYPE,
                PacketSellRequest.STREAM_CODEC,
                PacketSellRequest::handle);
        registrar.playToServer(
                PacketSetItemBuyerRecipe.TYPE,
                PacketSetItemBuyerRecipe.STREAM_CODEC,
                PacketSetItemBuyerRecipe::handle);
        registrar.playToServer(
                PacketSetFluidBuyerRecipe.TYPE,
                PacketSetFluidBuyerRecipe.STREAM_CODEC,
                PacketSetFluidBuyerRecipe::handle);
        registrar.playToServer(
                PacketAccountAddPermit.TYPE,
                PacketAccountAddPermit.STREAM_CODEC,
                PacketAccountAddPermit::handle);
        registrar.playToServer(
                PacketUpdateRequest.TYPE,
                PacketUpdateRequest.STREAM_CODEC,
                PacketUpdateRequest::handle);
        registrar.playToServer(
                PacketSetDetectorThreshold.TYPE,
                PacketSetDetectorThreshold.STREAM_CODEC,
                PacketSetDetectorThreshold::handle);
    }

    public static <MSG extends net.minecraft.network.protocol.common.custom.CustomPacketPayload> void sendToServer(MSG message) {
        PacketDistributor.sendToServer(message);
    }

    public static <MSG extends net.minecraft.network.protocol.common.custom.CustomPacketPayload> void sendToPlayer(MSG message, ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, message);
    }
}
