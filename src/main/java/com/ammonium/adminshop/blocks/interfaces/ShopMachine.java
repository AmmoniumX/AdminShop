package com.ammonium.adminshop.blocks.interfaces;

import net.minecraft.core.BlockPos;
import net.minecraft.world.MenuProvider;
import net.neoforged.neoforge.common.extensions.IBlockEntityExtension;

import java.util.UUID;

public interface ShopMachine extends MenuProvider, IBlockEntityExtension {
    void setTeamId(UUID teamId);
    UUID getTeamId();
    void sendUpdates();

    BlockPos getBlockPos();

    default boolean bypassesPermits() {
        return false;
    }
}
