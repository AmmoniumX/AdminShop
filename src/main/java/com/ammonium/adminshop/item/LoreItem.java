package com.ammonium.adminshop.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class LoreItem extends Item {
    private final Component loreText;
    public LoreItem(Properties pProperties, String lore) {
        super(pProperties);
        this.loreText = Component.literal(lore);
    }
    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(loreText);
    }
}
