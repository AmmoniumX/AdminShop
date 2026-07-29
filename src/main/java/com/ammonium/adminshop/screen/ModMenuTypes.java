package com.ammonium.adminshop.screen;

import com.ammonium.adminshop.AdminShop;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModMenuTypes {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(BuiltInRegistries.MENU, AdminShop.MODID);

    public static final DeferredHolder<MenuType<?>, MenuType<ShopMenu>> SHOP_MENU = MENUS.register("shop_menu",
            () -> IMenuTypeExtension.create(((windowId, inv, data) -> new ShopMenu(windowId, inv, inv.player))));

    public static final DeferredHolder<MenuType<?>, MenuType<SellerMenu>> SELLER_MENU = MENUS.register("seller_menu",
            () -> IMenuTypeExtension.create((SellerMenu::new)));

    public static final DeferredHolder<MenuType<?>, MenuType<BuyerMenu1>> BUYER_1_MENU = MENUS.register("buyer_menu",
            () -> IMenuTypeExtension.create((BuyerMenu1::new)));

    public static final DeferredHolder<MenuType<?>, MenuType<BuyerMenu2>> BUYER_2_MENU = MENUS.register("buyer_2_menu",
            () -> IMenuTypeExtension.create((BuyerMenu2::new)));

    public static final DeferredHolder<MenuType<?>, MenuType<BuyerMenu3>> BUYER_3_MENU = MENUS.register("buyer_3_menu",
            () -> IMenuTypeExtension.create((BuyerMenu3::new)));

    public static final DeferredHolder<MenuType<?>, MenuType<FluidBuyerMenu>> FLUID_BUYER_MENU = MENUS.register("fluid_buyer_menu",
            () -> IMenuTypeExtension.create((FluidBuyerMenu::new)));

    public static final DeferredHolder<MenuType<?>, MenuType<FluidSellerMenu>> FLUID_SELLER_MENU = MENUS.register("fluid_seller_menu",
            () -> IMenuTypeExtension.create((FluidSellerMenu::new)));

    public static final DeferredHolder<MenuType<?>, MenuType<CreativeBuyerMenu>> CREATIVE_BUYER_MENU = MENUS.register("creative_buyer_menu",
            () -> IMenuTypeExtension.create((CreativeBuyerMenu::new)));

    public static final DeferredHolder<MenuType<?>, MenuType<CreativeSellerMenu>> CREATIVE_SELLER_MENU = MENUS.register("creative_seller_menu",
            () -> IMenuTypeExtension.create((CreativeSellerMenu::new)));

    public static final DeferredHolder<MenuType<?>, MenuType<BasicDetectorMenu>> BASIC_DETECTOR_MENU = MENUS.register("detector_menu",
            () -> IMenuTypeExtension.create((BasicDetectorMenu::new)));

    public static final DeferredHolder<MenuType<?>, MenuType<AdvancedDetectorMenu>> ADVANCED_DETECTOR_MENU = MENUS.register("advanced_detector_menu",
            () -> IMenuTypeExtension.create((AdvancedDetectorMenu::new)));

    public static void register(IEventBus eventBus) {
        MENUS.register(eventBus);
    }
}
