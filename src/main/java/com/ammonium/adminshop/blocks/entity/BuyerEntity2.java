package com.ammonium.adminshop.blocks.entity;

import com.ammonium.adminshop.screen.BuyerMenu2;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class BuyerEntity2 extends AbstractBuyerEntity {
    private static final int SLOT_SIZE = 3;
    private static final int TICK_COOLDOWN = 20;

    public BuyerEntity2(BlockPos blockPos, BlockState blockState) {
        super(ModBlockEntities.BUYER_2.get(), BuyerMenu2::new, blockPos, blockState, SLOT_SIZE, TICK_COOLDOWN);
    }

}
