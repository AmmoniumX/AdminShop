package com.ammonium.adminshop.setup;

import com.ammonium.adminshop.AdminShop;
import com.ammonium.adminshop.client.KeyInit;
import com.ammonium.adminshop.screen.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = AdminShop.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public class ClientSetup {

    @SubscribeEvent
    public static void init(FMLClientSetupEvent event) {
        event.enqueueWork(KeyInit::init);
    }

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenuTypes.SHOP_MENU.get(), ShopScreen::new);
        event.register(ModMenuTypes.SELLER_MENU.get(), (SellerMenu menu, Inventory playerInventory, Component title) ->
                new SellerScreen(menu, playerInventory, title, menu.getBlockEntity().getBlockPos()));
        event.register(ModMenuTypes.BUYER_1_MENU.get(), (BuyerMenu1 menu, Inventory playerInventory, Component title) ->
                new BuyerScreen1(menu, playerInventory, title, menu.getBlockEntity().getBlockPos()));
        event.register(ModMenuTypes.BUYER_2_MENU.get(), (BuyerMenu2 menu, Inventory playerInventory, Component title) ->
                new BuyerScreen2(menu, playerInventory, title, menu.getBlockEntity().getBlockPos()));
        event.register(ModMenuTypes.BUYER_3_MENU.get(), (BuyerMenu3 menu, Inventory playerInventory, Component title) ->
                new BuyerScreen3(menu, playerInventory, title, menu.getBlockEntity().getBlockPos()));
        event.register(ModMenuTypes.FLUID_BUYER_MENU.get(), (FluidBuyerMenu menu, Inventory playerInventory, Component title) ->
                new FluidBuyerScreen(menu, playerInventory, title, menu.getBlockEntity().getBlockPos()));
        event.register(ModMenuTypes.FLUID_SELLER_MENU.get(), (FluidSellerMenu menu, Inventory playerInventory, Component title) ->
                new FluidSellerScreen(menu, playerInventory, title, menu.getBlockEntity().getBlockPos()));
        event.register(ModMenuTypes.BASIC_DETECTOR_MENU.get(), (BasicDetectorMenu menu, Inventory playerInventory, Component title) ->
                new BasicDetectorScreen(menu, playerInventory, title));
        event.register(ModMenuTypes.ADVANCED_DETECTOR_MENU.get(), (AdvancedDetectorMenu menu, Inventory playerInventory, Component title) ->
                new AdvancedDetectorScreen(menu, playerInventory, title));
    }
}
