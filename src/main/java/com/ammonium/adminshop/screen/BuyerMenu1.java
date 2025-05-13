package com.ammonium.adminshop.screen;

import com.ammonium.adminshop.blocks.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;

public class BuyerMenu1 extends AbstractBuyerMenu {
    private static final int SLOT_COUNT = 1;
    private static final int SLOT_START_X = 80;

    public BuyerMenu1(int windowId, Inventory inv, FriendlyByteBuf extraData) {
        super(ModMenuTypes.BUYER_1_MENU.get(), SLOT_COUNT, SLOT_START_X, windowId, inv, extraData);
    }

    public BuyerMenu1(int windowId, Inventory inv, BlockEntity blockEntity) {
        super(ModMenuTypes.BUYER_1_MENU.get(), SLOT_COUNT, SLOT_START_X, windowId, inv, blockEntity);
    }

    public BuyerMenu1(int id, Inventory playerInventory, Level pLevel, BlockPos pPos) {
        super(ModMenuTypes.BUYER_1_MENU.get(), SLOT_COUNT, SLOT_START_X, id, playerInventory, pLevel.getBlockEntity(pPos));
    }

    @Override
    protected Block getBlockType() {
        return ModBlocks.BUYER_1.get();
    }
}
