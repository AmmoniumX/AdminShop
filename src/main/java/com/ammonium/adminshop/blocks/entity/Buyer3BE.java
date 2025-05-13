package com.ammonium.adminshop.blocks.entity;

import com.ammonium.adminshop.screen.Buyer3Menu;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class Buyer3BE extends AbstractBuyerBE {
    private static final int SLOT_SIZE = 5;
    public static final int TICK_COOLDOWN = 10;

    public Buyer3BE(BlockPos blockPos, BlockState blockState) {
        super(ModBlockEntities.BUYER_3.get(), Buyer3Menu::new, blockPos, blockState, SLOT_SIZE, TICK_COOLDOWN);
    }

}
