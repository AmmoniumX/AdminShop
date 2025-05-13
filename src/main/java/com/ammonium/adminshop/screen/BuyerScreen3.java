package com.ammonium.adminshop.screen;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class BuyerScreen3 extends AbstractBuyerScreen<BuyerMenu3> {
    private static final String TEXTURE_PATH = "textures/gui/buyer_3.png";

    public BuyerScreen3(BuyerMenu3 menu, Inventory pPlayerInventory, Component pTitle, BlockPos blockPos) {
        super(TEXTURE_PATH, menu, pPlayerInventory, pTitle, blockPos);
    }
}
