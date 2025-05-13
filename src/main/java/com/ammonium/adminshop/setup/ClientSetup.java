package com.ammonium.adminshop.setup;

import com.ammonium.adminshop.AdminShop;
import com.ammonium.adminshop.client.KeyInit;
import com.ammonium.adminshop.screen.*;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = AdminShop.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ClientSetup {
    public static void init(FMLClientSetupEvent event){
        event.enqueueWork(() -> {
            MenuScreens.register(ModMenuTypes.SHOP_MENU.get(), ShopScreen::new);
            MenuScreens.<SellerMenu, SellerScreen>register(ModMenuTypes.SELLER_MENU.get(), (SellerMenu menu,
            Inventory playerInventory, Component title) -> new SellerScreen(menu, playerInventory, title,
                    menu.getBlockEntity().getBlockPos()));
            MenuScreens.<Buyer1Menu, Buyer1Screen>register(ModMenuTypes.BUYER_1_MENU.get(), (Buyer1Menu menu,
                                                                                             Inventory playerInventory, Component title) -> new Buyer1Screen(menu, playerInventory, title,
                    menu.getBlockEntity().getBlockPos()));
            MenuScreens.<Buyer2Menu, Buyer2Screen>register(ModMenuTypes.BUYER_2_MENU.get(), (Buyer2Menu menu,
            Inventory playerInventory, Component title) -> new Buyer2Screen(menu, playerInventory, title,
                    menu.getBlockEntity().getBlockPos()));
            MenuScreens.<Buyer3Menu, Buyer3Screen>register(ModMenuTypes.BUYER_3_MENU.get(), (Buyer3Menu menu,
            Inventory playerInventory, Component title) -> new Buyer3Screen(menu, playerInventory, title,
                    menu.getBlockEntity().getBlockPos()));
            MenuScreens.<FluidBuyerMenu, FluidBuyerScreen>register(ModMenuTypes.FLUID_BUYER_MENU.get(), (FluidBuyerMenu menu,
            Inventory playerInventory, Component title) -> new FluidBuyerScreen(menu, playerInventory, title,
                    menu.getBlockEntity().getBlockPos()));
            MenuScreens.<FluidSellerMenu, FluidSellerScreen>register(ModMenuTypes.FLUID_SELLER_MENU.get(), (FluidSellerMenu menu,
            Inventory playerInventory, Component title) -> new FluidSellerScreen(menu, playerInventory, title,
                    menu.getBlockEntity().getBlockPos()));
            MenuScreens.<BasicDetectorMenu, BasicDetectorScreen>register(ModMenuTypes.BASIC_DETECTOR_MENU.get(), BasicDetectorScreen::new);
            MenuScreens.<AdvancedDetectorMenu, AdvancedDetectorScreen>register(ModMenuTypes.ADVANCED_DETECTOR_MENU.get(), AdvancedDetectorScreen::new);
            KeyInit.init();
        });
    }
}
