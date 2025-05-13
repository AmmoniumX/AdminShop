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

    public static final RegistryObject<BlockEntityType<SellerBE>> SELLER =
            BLOCK_ENTITIES.register("seller", () -> BlockEntityType.Builder.of(SellerBE::new,
                    ModBlocks.SELLER.get()).build(null));

    public static final RegistryObject<BlockEntityType<ShopBE>> SHOP =
            BLOCK_ENTITIES.register("shop", () -> BlockEntityType.Builder.of(ShopBE::new,
                    ModBlocks.SHOP.get()).build(null));

    public static final RegistryObject<BlockEntityType<Buyer1BE>> BUYER_1 =
            BLOCK_ENTITIES.register("buyer_1", () -> BlockEntityType.Builder.of(Buyer1BE::new,
                    ModBlocks.BUYER_1.get()).build(null));

    public static final RegistryObject<BlockEntityType<Buyer2BE>> BUYER_2 =
            BLOCK_ENTITIES.register("buyer_2", () -> BlockEntityType.Builder.of(Buyer2BE::new,
                    ModBlocks.BUYER_2.get()).build(null));
    public static final RegistryObject<BlockEntityType<Buyer3BE>> BUYER_3 =
            BLOCK_ENTITIES.register("buyer_3", () -> BlockEntityType.Builder.of(Buyer3BE::new,
                    ModBlocks.BUYER_3.get()).build(null));

    public static final RegistryObject<BlockEntityType<FluidBuyerBE>> FLUID_BUYER =
            BLOCK_ENTITIES.register("fluid_buyer", () -> BlockEntityType.Builder.of(FluidBuyerBE::new,
                    ModBlocks.FLUID_BUYER.get()).build(null));

    public static final RegistryObject<BlockEntityType<FluidSellerBE>> FLUID_SELLER =
            BLOCK_ENTITIES.register("fluid_seller", () -> BlockEntityType.Builder.of(FluidSellerBE::new,
                    ModBlocks.FLUID_SELLER.get()).build(null));

    public static final RegistryObject<BlockEntityType<BasicDetectorBE>> BASIC_DETECTOR =
            BLOCK_ENTITIES.register("detector", () -> BlockEntityType.Builder.of(BasicDetectorBE::new,
                    ModBlocks.DETECTOR.get()).build(null));

    public static final RegistryObject<BlockEntityType<AdvancedDetectorBE>> ADVANCED_DETECTOR =
            BLOCK_ENTITIES.register("adv_detector", () -> BlockEntityType.Builder.of(AdvancedDetectorBE::new,
                    ModBlocks.ADVANCED_DETECTOR.get()).build(null));

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}
