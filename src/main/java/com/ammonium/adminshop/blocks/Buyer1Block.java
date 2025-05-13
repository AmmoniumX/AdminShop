package com.ammonium.adminshop.blocks;

import com.ammonium.adminshop.blocks.entity.AbstractBuyerBE;
import com.ammonium.adminshop.blocks.entity.Buyer1BE;
import com.ammonium.adminshop.blocks.entity.ModBlockEntities;
import com.ammonium.adminshop.screen.Buyer1Menu;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;

public class Buyer1Block extends AbstractBuyerBlock {
    public Buyer1Block() {
        super(Buyer1BE::new, Buyer1Menu::new);
    }

    @Override
    protected BlockEntityType<Buyer1BE> getBlockEntityType() {
        return ModBlockEntities.BUYER_1.get();
    }

    @Override
    protected <T extends AbstractBuyerBE> BlockEntityTicker<T> createBlockEntityTicker() {
        return Buyer1BE::tick;
    }

}