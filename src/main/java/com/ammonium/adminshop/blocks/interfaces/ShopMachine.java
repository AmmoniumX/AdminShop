package com.ammonium.adminshop.blocks.interfaces;

import net.minecraft.core.BlockPos;
import net.minecraft.world.MenuProvider;
import net.minecraftforge.common.extensions.IForgeBlockEntity;

import java.util.UUID;

public interface ShopMachine extends MenuProvider, IForgeBlockEntity {
    void setTeamId(UUID teamId);
    UUID getTeamId();
    void sendUpdates();

    BlockPos getBlockPos();
}
