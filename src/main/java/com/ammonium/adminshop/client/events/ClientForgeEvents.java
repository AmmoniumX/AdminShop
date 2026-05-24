package com.ammonium.adminshop.client.events;

import com.ammonium.adminshop.AdminShop;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(modid = AdminShop.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public final class ClientForgeEvents {

    private ClientForgeEvents(){}

    @SubscribeEvent
    public static void clientTick(ClientTickEvent.Post event){

    }
}
