package com.ammonium.adminshop.screen;

import com.ammonium.adminshop.blocks.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;

public class CreativeBuyerMenu extends AbstractBuyerMenu {
    private static final int SLOT_COUNT = 5;
    private static final int SLOT_START_X = 44;

    public CreativeBuyerMenu(int windowId, Inventory inv, FriendlyByteBuf extraData) {
        super(ModMenuTypes.CREATIVE_BUYER_MENU.get(), SLOT_COUNT, SLOT_START_X, windowId, inv, extraData);
    }

    public CreativeBuyerMenu(int windowId, Inventory inv, BlockEntity blockEntity) {
        super(ModMenuTypes.CREATIVE_BUYER_MENU.get(), SLOT_COUNT, SLOT_START_X, windowId, inv, blockEntity);
    }

    public CreativeBuyerMenu(int id, Inventory playerInventory, Level pLevel, BlockPos pPos) {
        super(ModMenuTypes.CREATIVE_BUYER_MENU.get(), SLOT_COUNT, SLOT_START_X, id, playerInventory, pLevel.getBlockEntity(pPos));
    }

    @Override
    protected Block getBlockType() {
        return ModBlocks.CREATIVE_BUYER.get();
    }
}
