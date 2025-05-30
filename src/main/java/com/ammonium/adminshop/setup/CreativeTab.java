package com.ammonium.adminshop.setup;

import com.ammonium.adminshop.AdminShop;
import com.ammonium.adminshop.blocks.ModBlocks;
import com.ammonium.adminshop.item.ModItems;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;

public class CreativeTab {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, AdminShop.MODID);

    public static void register(IEventBus bus) {
        CREATIVE_MODE_TABS.register(bus);
        CREATIVE_MODE_TABS.register("creativetab", () -> CreativeModeTab.builder()
                // Set name of tab to display
                .title(Component.translatable("item_group." + AdminShop.MODID + ".creativetab"))
                // Set icon of creative tab
                .icon(() -> new ItemStack(ModBlocks.SHOP.get()))
                // Add default items to tab
                .displayItems((params, output) -> {
//                    Registration.ITEMS.getEntries().forEach(e -> output.accept(e.get()));
                    ModItems.ITEMS.getEntries().forEach(e -> output.accept(new ItemStack(e.get())));
                    ModBlocks.BLOCKS.getEntries().forEach(e -> output.accept(new ItemStack(e.get())));
                })
                .build());
    }

}
