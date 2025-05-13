package com.ammonium.adminshop.blocks.entity;

import com.ammonium.adminshop.screen.Buyer2Menu;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class Buyer2BE extends AbstractBuyerBE {
    private static final int SLOT_SIZE = 3;
    private static final int TICK_COOLDOWN = 20;

    public Buyer2BE(BlockPos blockPos, BlockState blockState) {
        super(ModBlockEntities.BUYER_2.get(), Buyer2Menu::new, blockPos, blockState, SLOT_SIZE, TICK_COOLDOWN);
    }

}
