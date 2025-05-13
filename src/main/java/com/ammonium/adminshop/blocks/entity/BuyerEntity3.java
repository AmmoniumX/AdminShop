package com.ammonium.adminshop.blocks.entity;

import com.ammonium.adminshop.screen.BuyerMenu3;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class BuyerEntity3 extends AbstractBuyerEntity {
    private static final int SLOT_SIZE = 5;
    private static final int TICK_COOLDOWN = 10;

    public BuyerEntity3(BlockPos blockPos, BlockState blockState) {
        super(ModBlockEntities.BUYER_3.get(), BuyerMenu3::new, blockPos, blockState, SLOT_SIZE, TICK_COOLDOWN);
    }

}
