package com.ammonium.adminshop.blocks.entity;

import com.ammonium.adminshop.screen.BuyerMenu1;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class BuyerEntity1 extends AbstractBuyerEntity {
    private static final int SLOT_SIZE = 1;
    private static final int TICK_COOLDOWN = 40;

    public BuyerEntity1(BlockPos blockPos, BlockState blockState) {
        super(ModBlockEntities.BUYER_1.get(), BuyerMenu1::new, blockPos, blockState, SLOT_SIZE, TICK_COOLDOWN);
    }

}
