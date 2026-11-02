package com.ammonium.adminshop.screen;

import com.ammonium.adminshop.AdminShop;
import com.ammonium.adminshop.blocks.entity.AdvancedDetectorEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public class AdvancedDetectorScreen extends DetectorScreen<AdvancedDetectorMenu, AdvancedDetectorEntity> {
    private static final Identifier TEXTURE =
    Identifier.fromNamespaceAndPath(AdminShop.MODID, "textures/gui/adv_detector.png");

    public AdvancedDetectorScreen(AdvancedDetectorMenu pMenu, Inventory pPlayerInventory, Component pTitle, BlockPos blockPos) {
        super(pMenu, pPlayerInventory, pTitle, blockPos, AdvancedDetectorEntity.class);
    }

    public AdvancedDetectorScreen(AdvancedDetectorMenu pMenu, Inventory inventory, Component pTitle) {
        super(pMenu, inventory, pTitle, AdvancedDetectorEntity.class);
    }

    @Override
    protected Identifier getTexture() {
        return TEXTURE;
    }
}
