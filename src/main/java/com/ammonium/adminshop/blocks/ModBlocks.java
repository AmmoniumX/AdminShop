package com.ammonium.adminshop.blocks;

import com.ammonium.adminshop.AdminShop;
import com.ammonium.adminshop.item.LoreBlockItem;
import com.ammonium.adminshop.item.ModItems;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModBlocks {
//    public static final Material machineBlock = new Material(MaterialColor.METAL, false, true, true, true, false, false, PushReaction.BLOCK);
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(BuiltInRegistries.BLOCK, AdminShop.MODID);

    public static final DeferredHolder<Block, Block> SHOP = registerLoreBlock("shop",
            ShopBlock::new, "Buy and Sell Items!");
    public static final DeferredHolder<Block, BuyerBlock1> BUYER_1 = registerLoreBlock("buyer_1",
            BuyerBlock1::new, "Automatically buys once every 40 ticks");

    public static final DeferredHolder<Block, BuyerBlock2> BUYER_2 = registerLoreBlock("buyer_2",
            BuyerBlock2::new, "Automatically buys once every 20 ticks");
    public static final DeferredHolder<Block, BuyerBlock3> BUYER_3 = registerLoreBlock("buyer_3",
            BuyerBlock3::new, "Automatically buys once every 10 ticks");

    public static final DeferredHolder<Block, SellerBlock> SELLER = registerLoreBlock("seller",
            SellerBlock::new, "Automatically sells once every 20 ticks");

    public static final DeferredHolder<Block, CreativeBuyerBlock> CREATIVE_BUYER = registerLoreBlock("creative_buyer",
            CreativeBuyerBlock::new, "Set its recipe in creative mode, then claim it in survival to buy with that recipe, bypassing permits");

    public static final DeferredHolder<Block, CreativeSellerBlock> CREATIVE_SELLER = registerLoreBlock("creative_seller",
            CreativeSellerBlock::new, "Set its recipe in creative mode, then claim it in survival to sell with that recipe, bypassing permits");

    public static final DeferredHolder<Block, FluidBuyerBlock> FLUID_BUYER = registerLoreBlock("fluid_buyer",
            FluidBuyerBlock::new, "Automatically buys once every 20 ticks");

    public static final DeferredHolder<Block, FluidSellerBlock> FLUID_SELLER = registerLoreBlock("fluid_seller",
            FluidSellerBlock::new, "Automatically sells once every 20 ticks");

    public static final DeferredHolder<Block, BasicDetector> DETECTOR = registerLoreBlock("detector",
            BasicDetector::new, "Outputs full redstone signal if greater than threshold");

    public static final DeferredHolder<Block, AdvancedDetector> ADVANCED_DETECTOR = registerLoreBlock("adv_detector",
            AdvancedDetector::new, "Outputs analog redstone signal proportional on balance between 0 and threshold");

    private static <T extends Block> DeferredHolder<Block, T> registerBlock(String name, Supplier<T> block) {
        DeferredHolder<Block, T> toReturn = BLOCKS.register(name, block);
        registerBlockItem(name, toReturn);
        return toReturn;
    }

    private static <T extends Block> DeferredHolder<Item, BlockItem> registerBlockItem(String name, DeferredHolder<Block, T> block) {
        return ModItems.ITEMS.register(name, () -> new BlockItem(block.get(),
                new Item.Properties()));
    }

    private static <T extends Block> DeferredHolder<Block, T> registerLoreBlock(String name, Supplier<T> block, String lore) {
        DeferredHolder<Block, T> toReturn = BLOCKS.register(name, block);
        registerLoreBlockItem(name, toReturn, lore);
        return toReturn;
    }

    private static <T extends Block> DeferredHolder<Item, LoreBlockItem> registerLoreBlockItem(String name, DeferredHolder<Block, T> block,
                                                                                String lore) {
        return ModItems.ITEMS.register(name, () -> new LoreBlockItem(block.get(),
                new Item.Properties(), lore));
    }

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}