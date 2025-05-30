package com.ammonium.adminshop.setup;

import com.ammonium.adminshop.blocks.ModBlocks;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import org.jetbrains.annotations.NotNull;

public class ModSetup {
//    public static final String TAB_NAME = "adminshop";

//    public static final CreativeModeTab ITEM_GROUP = new CreativeModeTab(TAB_NAME) {
//        @Override
//        public @NotNull ItemStack makeIcon() {
//            return new ItemStack(ModBlocks.SHOP.get());
//        }
//    };
    public static void init(FMLCommonSetupEvent event){
        Messages.register();

    }
}
