package com.ammonium.adminshop.blocks;

import com.ammonium.adminshop.AdminShop;
import com.ammonium.adminshop.item.LoreBlockItem;
import com.ammonium.adminshop.item.ModItems;
import com.ammonium.adminshop.setup.ModSetup;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Supplier;

public class ModBlocks {
//    public static final Material machineBlock = new Material(MaterialColor.METAL, false, true, true, true, false, false, PushReaction.BLOCK);
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, AdminShop.MODID);

    public static final RegistryObject<Block> SHOP = registerLoreBlock("shop",
            ShopBlock::new, "Buy and Sell Items!");
    public static final RegistryObject<Block> BUYER_1 = registerLoreBlock("buyer_1",
            BuyerBlock1::new, "Automatically buys once every 40 ticks");

    public static final RegistryObject<Block> BUYER_2 = registerLoreBlock("buyer_2",
            BuyerBlock2::new, "Automatically buys once every 20 ticks");
    public static final RegistryObject<Block> BUYER_3 = registerLoreBlock("buyer_3",
            BuyerBlock3::new, "Automatically buys once every 10 ticks");

    public static final RegistryObject<Block> SELLER = registerLoreBlock("seller",
            SellerBlock::new, "Automatically sells once every 20 ticks");

    public static final RegistryObject<Block> CREATIVE_BUYER = registerLoreBlock("creative_buyer",
            CreativeBuyerBlock::new, "Set its recipe in creative mode, then claim it in survival to buy with that recipe, bypassing permits");

    public static final RegistryObject<Block> CREATIVE_SELLER = registerLoreBlock("creative_seller",
            CreativeSellerBlock::new, "Set its recipe in creative mode, then claim it in survival to sell with that recipe, bypassing permits");

    public static final RegistryObject<Block> FLUID_BUYER = registerLoreBlock("fluid_buyer",
            FluidBuyerBlock::new, "Automatically buys once every 20 ticks");

    public static final RegistryObject<Block> FLUID_SELLER = registerLoreBlock("fluid_seller",
            FluidSellerBlock::new, "Automatically sells once every 20 ticks");

    public static final RegistryObject<Block> DETECTOR = registerLoreBlock("detector",
            BasicDetector::new, "Outputs full redstone signal if greater than threshold");

    public static final RegistryObject<Block> ADVANCED_DETECTOR = registerLoreBlock("adv_detector",
            AdvancedDetector::new, "Outputs analog redstone signal proportional on balance between 0 and threshold");

    private static <T extends Block> RegistryObject<T> registerBlock(String name, Supplier<T> block) {
        RegistryObject<T> toReturn = BLOCKS.register(name, block);
        registerBlockItem(name, toReturn);
        return toReturn;
    }

    private static <T extends Block> RegistryObject<Item> registerBlockItem(String name, RegistryObject<T> block) {
        return ModItems.ITEMS.register(name, () -> new BlockItem(block.get(),
                new Item.Properties()));
    }

    private static <T extends Block> RegistryObject<T> registerLoreBlock(String name, Supplier<T> block, String lore) {
        RegistryObject<T> toReturn = BLOCKS.register(name, block);
        registerLoreBlockItem(name, toReturn, lore);
        return toReturn;
    }

    private static <T extends Block> RegistryObject<Item> registerLoreBlockItem(String name, RegistryObject<T> block,
                                                                                String lore) {
        return ModItems.ITEMS.register(name, () -> new LoreBlockItem(block.get(),
                new Item.Properties(), lore));
    }

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}