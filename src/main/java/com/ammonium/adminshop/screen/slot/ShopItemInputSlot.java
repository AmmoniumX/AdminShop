package com.ammonium.adminshop.screen.slot;

import com.ammonium.adminshop.recipes.RecipeManager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

public class ShopItemInputSlot extends SlotItemHandler {
    private final Level level;
    public ShopItemInputSlot(Level level, ItemStackHandler itemHandler, int index, int x, int y) {
        super(itemHandler, index, x, y);
        this.level = level;
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return RecipeManager.isSellItemRecipe(level, stack).isPresent();
    }
    @Override
    public boolean mayPickup(Player playerIn) {
        return true;
    }

    @Override
    public int getMaxStackSize(ItemStack pStack) {
        return super.getMaxStackSize(pStack);
    }

}
