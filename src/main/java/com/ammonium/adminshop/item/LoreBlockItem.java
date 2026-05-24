package com.ammonium.adminshop.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;

import java.util.List;

public class LoreBlockItem extends BlockItem {
    private final Component loreText;

    public LoreBlockItem(Block block, Properties properties, String loreText) {
        super(block, properties);
        this.loreText = Component.literal(loreText);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(loreText);
    }
}
