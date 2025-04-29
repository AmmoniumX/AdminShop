package com.ammonium.adminshop.client.events;

import com.ammonium.adminshop.AdminShop;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = AdminShop.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class ClientForgeEvents {

//    public static boolean isShopIncrease1Pressed = false;
//    public static boolean isShopIncrease2Pressed = false;

    private ClientForgeEvents(){}

    @SubscribeEvent
    public static void clientTick(TickEvent.ClientTickEvent event){

    }
}
