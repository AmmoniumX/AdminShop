package com.ammonium.adminshop.item;

import com.ammonium.adminshop.AdminShop;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.bus.api.IEventBus;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(BuiltInRegistries.ITEM, AdminShop.MODID);

    public static final DeferredHolder<Item, LoreItem> PERMIT = ITEMS.register("permit",
            () -> new LoreItem(new Item.Properties(), "Shift-click inside a shop to unlock new trades"));

    public static final DeferredHolder<Item, LoreItem> TABLET = ITEMS.register("tablet", ShopTablet::new);
    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}