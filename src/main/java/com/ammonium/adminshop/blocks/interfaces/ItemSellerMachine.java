package com.ammonium.adminshop.blocks.interfaces;

import net.neoforged.neoforge.items.IItemHandler;

public interface ItemSellerMachine extends ShopMachine {
    int getProgress();
    IItemHandler getItemHandler();
}
