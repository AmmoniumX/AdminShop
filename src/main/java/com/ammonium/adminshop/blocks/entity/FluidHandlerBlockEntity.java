package com.ammonium.adminshop.blocks.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import org.jetbrains.annotations.NotNull;

public class FluidHandlerBlockEntity extends BlockEntity {
    protected FluidTank tank = new FluidTank(FluidType.BUCKET_VOLUME);

    protected BlockCapabilityCache<IFluidHandler, Direction> fluidHandler;

    public FluidHandlerBlockEntity(@NotNull BlockEntityType<?> blockEntityType, BlockPos pos, BlockState state) {
        super(blockEntityType, pos, state);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (this.level == null || this.level.isClientSide()) {
            return;
        }
        this.fluidHandler = BlockCapabilityCache.create(
                Capabilities.FluidHandler.BLOCK, // capability to cache
                (ServerLevel) this.level, // level
                this.getBlockPos(), // target position
                Direction.UP // context
        );
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        tank.readFromNBT(provider, tag);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        tank.writeToNBT(provider, tag);
    }




}
