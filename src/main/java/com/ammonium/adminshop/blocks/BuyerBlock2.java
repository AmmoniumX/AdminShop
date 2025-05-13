package com.ammonium.adminshop.blocks;

import com.ammonium.adminshop.blocks.entity.AbstractBuyerEntity;
import com.ammonium.adminshop.blocks.entity.BuyerEntity2;
import com.ammonium.adminshop.blocks.entity.ModBlockEntities;
import com.ammonium.adminshop.screen.BuyerMenu2;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;

public class BuyerBlock2 extends AbstractBuyerBlock {
    public BuyerBlock2() {
        super(BuyerEntity2::new, BuyerMenu2::new);
    }

    @Override
    protected BlockEntityType<BuyerEntity2> getBlockEntityType() {
        return ModBlockEntities.BUYER_2.get();
    }

    @Override
    protected <T extends AbstractBuyerEntity> BlockEntityTicker<T> createBlockEntityTicker() {
        return BuyerEntity2::tick;
    }

}