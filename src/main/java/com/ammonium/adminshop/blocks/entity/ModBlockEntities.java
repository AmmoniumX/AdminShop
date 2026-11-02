package com.ammonium.adminshop.blocks.entity;

import com.ammonium.adminshop.AdminShop;
import com.ammonium.adminshop.blocks.ModBlocks;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

@EventBusSubscriber(modid = AdminShop.MODID)
public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, AdminShop.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SellerEntity>> SELLER =
            BLOCK_ENTITIES.register("seller", () -> new BlockEntityType<>(SellerEntity::new,
                    ModBlocks.SELLER.get()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ShopEntity>> SHOP =
            BLOCK_ENTITIES.register("shop", () -> new BlockEntityType<>(ShopEntity::new,
                    ModBlocks.SHOP.get()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BuyerEntity1>> BUYER_1 =
            BLOCK_ENTITIES.register("buyer_1", () -> new BlockEntityType<>(BuyerEntity1::new,
                    ModBlocks.BUYER_1.get()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BuyerEntity2>> BUYER_2 =
            BLOCK_ENTITIES.register("buyer_2", () -> new BlockEntityType<>(BuyerEntity2::new,
                    ModBlocks.BUYER_2.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BuyerEntity3>> BUYER_3 =
            BLOCK_ENTITIES.register("buyer_3", () -> new BlockEntityType<>(BuyerEntity3::new,
                    ModBlocks.BUYER_3.get()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FluidBuyerEntity>> FLUID_BUYER =
            BLOCK_ENTITIES.register("fluid_buyer", () -> new BlockEntityType<>(FluidBuyerEntity::new,
                    ModBlocks.FLUID_BUYER.get()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FluidSellerEntity>> FLUID_SELLER =
            BLOCK_ENTITIES.register("fluid_seller", () -> new BlockEntityType<>(FluidSellerEntity::new,
                    ModBlocks.FLUID_SELLER.get()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CreativeBuyerEntity>> CREATIVE_BUYER =
            BLOCK_ENTITIES.register("creative_buyer", () -> new BlockEntityType<>(CreativeBuyerEntity::new,
                    ModBlocks.CREATIVE_BUYER.get()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CreativeSellerEntity>> CREATIVE_SELLER =
            BLOCK_ENTITIES.register("creative_seller", () -> new BlockEntityType<>(CreativeSellerEntity::new,
                    ModBlocks.CREATIVE_SELLER.get()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BasicDetectorEntity>> BASIC_DETECTOR =
            BLOCK_ENTITIES.register("detector", () -> new BlockEntityType<>(BasicDetectorEntity::new,
                    ModBlocks.DETECTOR.get()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AdvancedDetectorEntity>> ADVANCED_DETECTOR =
            BLOCK_ENTITIES.register("adv_detector", () -> new BlockEntityType<>(AdvancedDetectorEntity::new,
                    ModBlocks.ADVANCED_DETECTOR.get()));

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }

    @SubscribeEvent
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {

        // Seller
        event.registerBlockEntity(
                Capabilities.Item.BLOCK,
                ModBlockEntities.SELLER.get(),
                (be, side) -> be.getInventory() // Assuming you add a getter
        );

        // Creative Seller
        event.registerBlockEntity(
                Capabilities.Item.BLOCK,
                ModBlockEntities.CREATIVE_SELLER.get(),
                (be, side) -> be.getInventory()
        );

        // Fluid Buyer
        event.registerBlockEntity(
                Capabilities.Fluid.BLOCK,
                ModBlockEntities.FLUID_BUYER.get(),
                (be, side) -> be.getTank() // Assuming you add a getter
        );

        // Fluid Seller
        event.registerBlockEntity(
                Capabilities.Fluid.BLOCK,
                ModBlockEntities.FLUID_SELLER.get(),
                (be, side) -> be.getTank()
        );
    }
}
