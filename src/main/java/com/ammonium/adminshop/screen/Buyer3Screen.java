package com.ammonium.adminshop.screen;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class Buyer3Screen extends AbstractBuyerScreen<Buyer3Menu> {
    private static final String TEXTURE_PATH = "textures/gui/buyer_3.png";

    public Buyer3Screen(Buyer3Menu menu, Inventory pPlayerInventory, Component pTitle, BlockPos blockPos) {
        super(TEXTURE_PATH, menu, pPlayerInventory, pTitle, blockPos);
    }
}
