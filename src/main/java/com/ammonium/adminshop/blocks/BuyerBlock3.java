package com.ammonium.adminshop.blocks;

import com.ammonium.adminshop.blocks.entity.AbstractBuyerEntity;
import com.ammonium.adminshop.blocks.entity.BuyerEntity3;
import com.ammonium.adminshop.blocks.entity.ModBlockEntities;
import com.ammonium.adminshop.screen.BuyerMenu3;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;

public class BuyerBlock3 extends AbstractBuyerBlock {
    public BuyerBlock3() {
        super(BuyerEntity3::new, BuyerMenu3::new);
    }

    @Override
    protected BlockEntityType<BuyerEntity3> getBlockEntityType() {
        return ModBlockEntities.BUYER_3.get();
    }

    @Override
    protected <T extends AbstractBuyerEntity> BlockEntityTicker<T> createBlockEntityTicker() {
        return BuyerEntity3::tick;
    }

}