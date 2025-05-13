package com.ammonium.adminshop.blocks.entity;

import com.ammonium.adminshop.AdminShop;
import com.ammonium.adminshop.blocks.ModBlocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, AdminShop.MODID);

    public static final RegistryObject<BlockEntityType<SellerEntity>> SELLER =
            BLOCK_ENTITIES.register("seller", () -> BlockEntityType.Builder.of(SellerEntity::new,
                    ModBlocks.SELLER.get()).build(null));

    public static final RegistryObject<BlockEntityType<ShopEntity>> SHOP =
            BLOCK_ENTITIES.register("shop", () -> BlockEntityType.Builder.of(ShopEntity::new,
                    ModBlocks.SHOP.get()).build(null));

    public static final RegistryObject<BlockEntityType<BuyerEntity1>> BUYER_1 =
            BLOCK_ENTITIES.register("buyer_1", () -> BlockEntityType.Builder.of(BuyerEntity1::new,
                    ModBlocks.BUYER_1.get()).build(null));

    public static final RegistryObject<BlockEntityType<BuyerEntity2>> BUYER_2 =
            BLOCK_ENTITIES.register("buyer_2", () -> BlockEntityType.Builder.of(BuyerEntity2::new,
                    ModBlocks.BUYER_2.get()).build(null));
    public static final RegistryObject<BlockEntityType<BuyerEntity3>> BUYER_3 =
            BLOCK_ENTITIES.register("buyer_3", () -> BlockEntityType.Builder.of(BuyerEntity3::new,
                    ModBlocks.BUYER_3.get()).build(null));

    public static final RegistryObject<BlockEntityType<FluidBuyerEntity>> FLUID_BUYER =
            BLOCK_ENTITIES.register("fluid_buyer", () -> BlockEntityType.Builder.of(FluidBuyerEntity::new,
                    ModBlocks.FLUID_BUYER.get()).build(null));

    public static final RegistryObject<BlockEntityType<FluidSellerEntity>> FLUID_SELLER =
            BLOCK_ENTITIES.register("fluid_seller", () -> BlockEntityType.Builder.of(FluidSellerEntity::new,
                    ModBlocks.FLUID_SELLER.get()).build(null));

    public static final RegistryObject<BlockEntityType<BasicDetectorEntity>> BASIC_DETECTOR =
            BLOCK_ENTITIES.register("detector", () -> BlockEntityType.Builder.of(BasicDetectorEntity::new,
                    ModBlocks.DETECTOR.get()).build(null));

    public static final RegistryObject<BlockEntityType<AdvancedDetectorEntity>> ADVANCED_DETECTOR =
            BLOCK_ENTITIES.register("adv_detector", () -> BlockEntityType.Builder.of(AdvancedDetectorEntity::new,
                    ModBlocks.ADVANCED_DETECTOR.get()).build(null));

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}
