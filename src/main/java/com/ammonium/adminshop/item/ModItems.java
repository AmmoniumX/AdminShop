package com.ammonium.adminshop.item;

import com.ammonium.adminshop.AdminShop;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.bus.api.IEventBus;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(BuiltInRegistries.ITEM, AdminShop.MODID);

    public static final DeferredHolder<Item, LoreItem> PERMIT = ITEMS.register("permit",
            id -> new LoreItem(new Item.Properties().setId(ResourceKey.create(Registries.ITEM, id)), "Shift-click inside a shop to unlock new trades"));

    public static final DeferredHolder<Item, LoreItem> TABLET = ITEMS.register("tablet", id -> new ShopTablet(new Item.Properties().setId(ResourceKey.create(Registries.ITEM, id))));
    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}