package com.ammonium.adminshop.screen;

import com.ammonium.adminshop.AdminShop;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.neoforged.bus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModMenuTypes {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, AdminShop.MODID);

    public static final RegistryObject<MenuType<ShopMenu>> SHOP_MENU = MENUS.register("shop_menu",
            () -> IForgeMenuType.create(((windowId, inv, data) -> new ShopMenu(windowId, inv, inv.player))));

    public static final RegistryObject<MenuType<SellerMenu>> SELLER_MENU = MENUS.register("seller_menu",
            () -> IForgeMenuType.create((SellerMenu::new)));

    public static final RegistryObject<MenuType<BuyerMenu1>> BUYER_1_MENU = MENUS.register("buyer_menu",
            () -> IForgeMenuType.create((BuyerMenu1::new)));

    public static final RegistryObject<MenuType<BuyerMenu2>> BUYER_2_MENU = MENUS.register("buyer_2_menu",
            () -> IForgeMenuType.create((BuyerMenu2::new)));

    public static final RegistryObject<MenuType<BuyerMenu3>> BUYER_3_MENU = MENUS.register("buyer_3_menu",
            () -> IForgeMenuType.create((BuyerMenu3::new)));

    public static final RegistryObject<MenuType<FluidBuyerMenu>> FLUID_BUYER_MENU = MENUS.register("fluid_buyer_menu",
            () -> IForgeMenuType.create((FluidBuyerMenu::new)));
    public static final RegistryObject<MenuType<FluidSellerMenu>> FLUID_SELLER_MENU = MENUS.register("fluid_seller_menu",
            () -> IForgeMenuType.create((FluidSellerMenu::new)));

    public static final RegistryObject<MenuType<BasicDetectorMenu>> BASIC_DETECTOR_MENU = MENUS.register("detector_menu",
            () -> IForgeMenuType.create((BasicDetectorMenu::new)));

    public static final RegistryObject<MenuType<AdvancedDetectorMenu>> ADVANCED_DETECTOR_MENU = MENUS.register("advanced_detector_menu",
            () -> IForgeMenuType.create((AdvancedDetectorMenu::new)));

    public static void register(IEventBus eventBus) {
        MENUS.register(eventBus);
    }
}
