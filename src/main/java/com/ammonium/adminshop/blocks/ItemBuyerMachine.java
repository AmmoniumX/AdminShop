package com.ammonium.adminshop.blocks;

import com.ammonium.adminshop.shop.ShopItem;
import net.minecraftforge.items.ItemStackHandler;

public interface ItemBuyerMachine extends ShopMachine {
    ItemStackHandler getItemHandler();
    void setTargetShopItem(ShopItem item);
    // TODO change to setTargetShopRecipe(ShopRecipe recipe)
}
