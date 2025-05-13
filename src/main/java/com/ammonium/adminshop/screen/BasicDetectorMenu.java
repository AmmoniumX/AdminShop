package com.ammonium.adminshop.screen;

import com.ammonium.adminshop.blocks.entity.BasicDetectorEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public class BasicDetectorMenu extends DetectorMenu<BasicDetectorEntity> {
    public BasicDetectorMenu(int windowId, Inventory inv, FriendlyByteBuf extraData) {
        super(windowId, inv, extraData, ModMenuTypes.BASIC_DETECTOR_MENU.get(), BasicDetectorEntity.class);
    }

    public BasicDetectorMenu(int windowId, Inventory inv, BlockEntity entity) {
        super(windowId, inv, entity, ModMenuTypes.BASIC_DETECTOR_MENU.get(), BasicDetectorEntity.class);
    }
    public BasicDetectorMenu(int id, Inventory playerInventory, Level pLevel, BlockPos pPos) {
        this(id, playerInventory, pLevel.getBlockEntity(pPos));
    }
}
