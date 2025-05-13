package com.ammonium.adminshop.blocks;

import com.ammonium.adminshop.AdminShop;
import com.ammonium.adminshop.item.LoreBlockItem;
import com.ammonium.adminshop.item.ModItems;
import com.ammonium.adminshop.setup.ModSetup;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.level.material.MaterialColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Supplier;

public class ModBlocks {
    public static final Material machineBlock = new Material(MaterialColor.METAL, false, true, true, true, false, false, PushReaction.BLOCK);
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, AdminShop.MODID);

    public static final RegistryObject<Block> SHOP = registerLoreBlock("shop",
            ShopBlock::new, ModSetup.ITEM_GROUP, "Buy and Sell Items!");
    public static final RegistryObject<Block> BUYER_1 = registerLoreBlock("buyer_1",
            Buyer1Block::new, ModSetup.ITEM_GROUP, "Automatically buys once every 40 ticks");

    public static final RegistryObject<Block> BUYER_2 = registerLoreBlock("buyer_2",
            Buyer2Block::new, ModSetup.ITEM_GROUP, "Automatically buys once every 20 ticks");
    public static final RegistryObject<Block> BUYER_3 = registerLoreBlock("buyer_3",
            Buyer3Block::new, ModSetup.ITEM_GROUP, "Automatically buys once every 10 ticks");

    public static final RegistryObject<Block> SELLER = registerLoreBlock("seller",
            SellerBlock::new, ModSetup.ITEM_GROUP, "Automatically sells once every 20 ticks");

    public static final RegistryObject<Block> FLUID_BUYER = registerLoreBlock("fluid_buyer",
            FluidBuyerBlock::new, ModSetup.ITEM_GROUP, "Automatically buys once every 20 ticks");

    public static final RegistryObject<Block> FLUID_SELLER = registerLoreBlock("fluid_seller",
            FluidSellerBlock::new, ModSetup.ITEM_GROUP, "Automatically sells once every 20 ticks");

    public static final RegistryObject<Block> DETECTOR = registerLoreBlock("detector",
            BasicDetector::new, ModSetup.ITEM_GROUP, "Outputs full redstone signal if greater than threshold");

    public static final RegistryObject<Block> ADVANCED_DETECTOR = registerLoreBlock("adv_detector",
            AdvancedDetector::new, ModSetup.ITEM_GROUP, "Outputs analog redstone signal proportional on balance between 0 and threshold");

    private static <T extends Block> RegistryObject<T> registerBlock(String name, Supplier<T> block, CreativeModeTab tab) {
        RegistryObject<T> toReturn = BLOCKS.register(name, block);
        registerBlockItem(name, toReturn, tab);
        return toReturn;
    }

    private static <T extends Block> RegistryObject<Item> registerBlockItem(String name, RegistryObject<T> block,
                                                                            CreativeModeTab tab) {
        return ModItems.ITEMS.register(name, () -> new BlockItem(block.get(),
                new Item.Properties().tab(tab)));
    }

    private static <T extends Block> RegistryObject<T> registerLoreBlock(String name, Supplier<T> block, CreativeModeTab tab,
                                                                         String lore) {
        RegistryObject<T> toReturn = BLOCKS.register(name, block);
        registerLoreBlockItem(name, toReturn, tab, lore);
        return toReturn;
    }

    private static <T extends Block> RegistryObject<Item> registerLoreBlockItem(String name, RegistryObject<T> block,
                                                                                CreativeModeTab tab, String lore) {
        return ModItems.ITEMS.register(name, () -> new LoreBlockItem(block.get(),
                new Item.Properties().tab(tab), lore));
    }

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}