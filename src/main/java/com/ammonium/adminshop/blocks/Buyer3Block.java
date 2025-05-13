package com.ammonium.adminshop.blocks;

import com.ammonium.adminshop.blocks.entity.AbstractBuyerBE;
import com.ammonium.adminshop.blocks.entity.Buyer3BE;
import com.ammonium.adminshop.blocks.entity.ModBlockEntities;
import com.ammonium.adminshop.screen.Buyer3Menu;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;

public class Buyer3Block extends AbstractBuyerBlock {
    public Buyer3Block() {
        super(Buyer3BE::new, Buyer3Menu::new);
    }

    @Override
    protected BlockEntityType<Buyer3BE> getBlockEntityType() {
        return ModBlockEntities.BUYER_3.get();
    }

    @Override
    protected <T extends AbstractBuyerBE> BlockEntityTicker<T> createBlockEntityTicker() {
        return Buyer3BE::tick;
    }

}