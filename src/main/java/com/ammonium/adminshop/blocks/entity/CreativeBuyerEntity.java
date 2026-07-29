package com.ammonium.adminshop.blocks.entity;

import com.ammonium.adminshop.screen.CreativeBuyerMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class CreativeBuyerEntity extends AbstractBuyerEntity {
    private static final int SLOT_SIZE = 5;
    private static final int TICK_COOLDOWN = 10;

    public CreativeBuyerEntity(BlockPos blockPos, BlockState blockState) {
        super(ModBlockEntities.CREATIVE_BUYER.get(), CreativeBuyerMenu::new, blockPos, blockState, SLOT_SIZE, TICK_COOLDOWN);
        super.setLockedRecipe(true);
    }

    @Override
    public boolean bypassesPermits() {
        return true;
    }
}
