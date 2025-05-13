package com.ammonium.adminshop.blocks.entity;

import com.ammonium.adminshop.screen.Buyer1Menu;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class Buyer1BE extends AbstractBuyerBE {
    private static final int SLOT_SIZE = 1;
    private static final int TICK_COOLDOWN = 40;

    public Buyer1BE(BlockPos blockPos, BlockState blockState) {
        super(ModBlockEntities.BUYER_1.get(), Buyer1Menu::new, blockPos, blockState, SLOT_SIZE, TICK_COOLDOWN);
    }

}
