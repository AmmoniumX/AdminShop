package com.ammonium.adminshop.blocks;

import com.ammonium.adminshop.shop.ShopItem;
import net.minecraft.world.Container;

public interface ItemBuyerMachine extends Container, ShopMachine {
    void setTargetShopItem(ShopItem item);
    // TODO change to setTargetShopRecipe(ShopRecipe recipe)
}
