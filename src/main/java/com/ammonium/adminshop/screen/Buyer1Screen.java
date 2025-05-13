package com.ammonium.adminshop.screen;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class Buyer1Screen extends AbstractBuyerScreen<Buyer1Menu> {
    private static final String TEXTURE_PATH = "textures/gui/buyer.png";

    public Buyer1Screen(Buyer1Menu menu, Inventory pPlayerInventory, Component pTitle, BlockPos blockPos) {
        super(TEXTURE_PATH, menu, pPlayerInventory, pTitle, blockPos);
    }
}
