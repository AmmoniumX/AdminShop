package com.ammonium.adminshop.blocks;

import com.ammonium.adminshop.blocks.entity.CreativeSellerEntity;
import com.ammonium.adminshop.blocks.entity.ModBlockEntities;
import com.ammonium.adminshop.blocks.entity.SellerEntity;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class CreativeSellerBlock extends SellerBlock {
    public static final MapCodec<CreativeSellerBlock> CODEC = simpleCodec(p -> new CreativeSellerBlock());

    @Override
    public MapCodec<CreativeSellerBlock> codec() { return CODEC; }

    public CreativeSellerBlock() {
        super(defaultProperties().strength(-1.0F, 3600000.0F));
    }

    @Override
    protected void assignInitialTeamId(ServerLevel serverLevel, ServerPlayer serverPlayer, SellerEntity sellerEntity) {
        // Creative Sellers are placed unclaimed; leave the teamId unset so the placer can
        // lock in a recipe before anyone claims it. Open the menu right away for that purpose.
        // The next right-click (by anyone, including the placer) goes through the normal
        // claim-on-first-use flow inherited from SellerBlock.useWithoutItem.
        serverPlayer.openMenu(sellerEntity, sellerEntity.getBlockPos());
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pPos, BlockState pState) {
        return new CreativeSellerEntity(pPos, pState);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level pLevel, BlockState pState, BlockEntityType<T> pBlockEntityType) {
        return pLevel.isClientSide() ? null : checkType(pBlockEntityType, ModBlockEntities.CREATIVE_SELLER.get(),
                (level, pos, state, blockEntity) -> SellerEntity.tick(level, pos, state, (SellerEntity) blockEntity));
    }

    private static <T extends BlockEntity> BlockEntityTicker<T> checkType(BlockEntityType<T> blockEntityType, BlockEntityType<?> expectedType, BlockEntityTicker<? super T> ticker) {
        return blockEntityType == expectedType ? (BlockEntityTicker<T>) ticker : null;
    }
}
