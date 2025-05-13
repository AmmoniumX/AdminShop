package com.ammonium.adminshop.screen;

import com.ammonium.adminshop.AdminShop;
import com.ammonium.adminshop.blocks.entity.BasicDetectorEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class BasicDetectorScreen extends DetectorScreen<BasicDetectorMenu, BasicDetectorEntity> {
    private static final ResourceLocation TEXTURE =
    new ResourceLocation(AdminShop.MODID, "textures/gui/detector.png");

    public BasicDetectorScreen(BasicDetectorMenu pMenu, Inventory pPlayerInventory, Component pTitle, BlockPos blockPos) {
        super(pMenu, pPlayerInventory, pTitle, blockPos, BasicDetectorEntity.class);
    }

    public BasicDetectorScreen(BasicDetectorMenu pMenu, Inventory inventory, Component pTitle) {
        super(pMenu, inventory, pTitle, BasicDetectorEntity.class);
    }

    @Override
    protected ResourceLocation getTexture() {
        return TEXTURE;
    }
}
