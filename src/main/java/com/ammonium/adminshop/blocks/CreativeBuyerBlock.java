package com.ammonium.adminshop.blocks;

import com.ammonium.adminshop.blocks.entity.AbstractBuyerEntity;
import com.ammonium.adminshop.blocks.entity.CreativeBuyerEntity;
import com.ammonium.adminshop.blocks.entity.ModBlockEntities;
import com.ammonium.adminshop.screen.CreativeBuyerMenu;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;

public class CreativeBuyerBlock extends AbstractBuyerBlock {
    public CreativeBuyerBlock() {
        super(CreativeBuyerEntity::new, CreativeBuyerMenu::new,
                defaultProperties().strength(-1.0F, 3600000.0F));
    }

    @Override
    protected BlockEntityType<CreativeBuyerEntity> getBlockEntityType() {
        return ModBlockEntities.CREATIVE_BUYER.get();
    }

    @Override
    protected <T extends AbstractBuyerEntity> BlockEntityTicker<T> createBlockEntityTicker() {
        return CreativeBuyerEntity::tick;
    }

    @Override
    protected void assignInitialTeamId(ServerLevel serverLevel, ServerPlayer serverPlayer, AbstractBuyerEntity buyerEntity) {
        // Creative Buyers are placed unclaimed; leave the teamId unset so the placer can
        // lock in a recipe before anyone claims it. Open the menu right away for that purpose.
        // The next right-click (by anyone, including the placer) goes through the normal
        // claim-on-first-use flow inherited from AbstractBuyerBlock.useItemOn.
        serverPlayer.openMenu(buyerEntity, buyerEntity.getBlockPos());
    }
}
