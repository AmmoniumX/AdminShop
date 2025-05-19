package com.ammonium.adminshop.blocks.interfaces;

import net.minecraft.world.Container;

public interface ItemSellerMachine extends Container, ShopMachine {
    int getProgress();
}
