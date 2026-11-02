package com.ammonium.adminshop.blocks;

import com.ammonium.adminshop.blocks.entity.AbstractBuyerEntity;
import com.ammonium.adminshop.blocks.entity.BuyerEntity1;
import com.ammonium.adminshop.blocks.entity.ModBlockEntities;
import com.ammonium.adminshop.screen.BuyerMenu1;
import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;

public class BuyerBlock1 extends AbstractBuyerBlock {
    public BuyerBlock1(BlockBehaviour.Properties properties) {
        super(BuyerEntity1::new, BuyerMenu1::new, defaultProperties(properties));
    }

    @Override
    protected BlockEntityType<BuyerEntity1> getBlockEntityType() {
        return ModBlockEntities.BUYER_1.get();
    }

    @Override
    protected <T extends AbstractBuyerEntity> BlockEntityTicker<T> createBlockEntityTicker() {
        return BuyerEntity1::tick;
    }
}