package com.ammonium.adminshop.blocks.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class ShopEntity extends BlockEntity {

    public ShopEntity(BlockPos pos, BlockState state){
        super(ModBlockEntities.SHOP.get(), pos, state);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
    }

}
