package com.ammonium.adminshop.item;

import com.ammonium.adminshop.AdminShop;
import com.ammonium.adminshop.setup.ModSetup;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, AdminShop.MODID);

    public static final RegistryObject<Item> PERMIT = ITEMS.register("permit",
            () -> new LoreItem(new Item.Properties(), "Shift-click inside a shop to unlock new trades"));

    public static final RegistryObject<Item> TABLET = ITEMS.register("tablet", ShopTablet::new);
    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}