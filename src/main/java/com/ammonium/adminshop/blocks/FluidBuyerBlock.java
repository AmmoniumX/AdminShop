package com.ammonium.adminshop.blocks;

import com.ammonium.adminshop.AdminShop;
import com.ammonium.adminshop.blocks.entity.FluidBuyerEntity;
import com.ammonium.adminshop.blocks.entity.ModBlockEntities;
import com.ammonium.adminshop.money.MoneyHelper;
import com.ammonium.adminshop.screen.FluidBuyerMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.BlockHitResult;
import com.mojang.serialization.MapCodec;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class FluidBuyerBlock extends BaseEntityBlock {
    public static final MapCodec<FluidBuyerBlock> CODEC = simpleCodec(p -> new FluidBuyerBlock());

    @Override
    public MapCodec<FluidBuyerBlock> codec() { return CODEC; }

    public FluidBuyerBlock() {
        super(BlockBehaviour.Properties.of()
                .mapColor(MapColor.METAL)
                .sound(SoundType.METAL)
                .strength(1.0f)
                .lightLevel(state -> 0)
                .dynamicShape()
                .forceSolidOn()
                .noOcclusion()
                .pushReaction(PushReaction.BLOCK)
        );
    }

//    private static final VoxelShape RENDER_SHAPE = Shapes.block();
    @Override
    public void onRemove(BlockState pState, Level pLevel, BlockPos pPos, BlockState pNewState, boolean pIsMoving) {
        if (pState.getBlock() != pNewState.getBlock()) {
            BlockEntity blockEntity = pLevel.getBlockEntity(pPos);
            if (blockEntity instanceof FluidBuyerEntity fbuyerEntity) {
                fbuyerEntity.setRemoved();
            }
        }
        super.onRemove(pState, pLevel, pPos, pNewState, pIsMoving);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState pState, Level pLevel, BlockPos pPos,
                                 Player pPlayer, BlockHitResult pHit) {
        if (!pLevel.isClientSide()) {

            ServerLevel serverLevel = (ServerLevel) pLevel;
            if(pLevel.getBlockEntity(pPos) instanceof FluidBuyerEntity fbuyerEntity
                && pPlayer instanceof ServerPlayer serverPlayer) {
                @Nullable UUID teamId = fbuyerEntity.getTeamId();
                if (teamId == null) {
                    AdminShop.LOGGER.debug("Claiming unclaimed machine for {}", serverPlayer.getName().getString());
                    @Nullable UUID newTeamId = MoneyHelper.get(serverLevel).getTeamUUIDForPlayer(serverPlayer);
                    if (newTeamId == null) {
                        AdminShop.LOGGER.debug("Could not find FTB team for {}", serverPlayer.getName().getString());
                    } else {
                        fbuyerEntity.setTeamId(newTeamId);
                        pPlayer.playNotifySound(SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0f, 1.0f);
                        pPlayer.sendSystemMessage(Component.translatable("message.adminshop.claimed_machine"));
                        serverPlayer.openMenu(fbuyerEntity, pPos);
                    }

                } else if (MoneyHelper.get(serverLevel).isMemberOfTeam(teamId, serverPlayer)) {
                    // Open menu
                    serverPlayer.openMenu(fbuyerEntity, pPos);
                } else {
                    // No access
                    pPlayer.sendSystemMessage(Component.translatable("message.adminshop.no_access"));
                }

            } else {
                throw new IllegalStateException("Our Container provider is missing!");
            }
        }
        return InteractionResult.sidedSuccess(pLevel.isClientSide());
    }

//    @Override
//    public VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
//        return RENDER_SHAPE;
//    }

    @Nullable
    @Override
    public MenuProvider getMenuProvider(BlockState pState, Level pLevel, BlockPos pPos) {
        return new SimpleMenuProvider((id, playerInventory, player) -> new FluidBuyerMenu(id, playerInventory, pLevel, pPos), Component.translatable("screen.adminshop.buyer"));
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pPos, BlockState pState) {
        return new FluidBuyerEntity(pPos, pState);
    }

    @Override
    public RenderShape getRenderShape(BlockState pState) {
        return RenderShape.MODEL;
    }

    @Override
    public void setPlacedBy(Level pLevel, BlockPos pPos, BlockState pState, @Nullable LivingEntity pPlacer, ItemStack pStack) {
        super.setPlacedBy(pLevel, pPos, pState, pPlacer, pStack);
        if (!pLevel.isClientSide) {
            // Server side code
            ServerLevel serverLevel = (ServerLevel) pLevel;
            BlockEntity blockEntity = pLevel.getBlockEntity(pPos);
            // Set initial values
            if (pPlacer instanceof ServerPlayer serverPlayer && blockEntity instanceof FluidBuyerEntity fbuyerEntity) {
                fbuyerEntity.setTeamId(MoneyHelper.get(serverLevel).getPlayerAccount(serverPlayer).teamId());
                fbuyerEntity.setChanged();
                fbuyerEntity.sendUpdates();
            }
        }
    }

//    @Override
//    public BlockState getStateForPlacement(BlockPlaceContext pContext) {
//        return this.defaultBlockState().setValue(FACING, pContext.getHorizontalDirection().getOpposite());
//    }
//
//    @Override
//    public BlockState rotate(BlockState pState, Rotation pRotation) {
//        return pState.setValue(FACING, pRotation.rotate(pState.getValue(FACING)));
//    }
//
//    @Override
//    public BlockState mirror(BlockState pState, Mirror pMirror) {
//        return pState.rotate(pMirror.getRotation(pState.getValue(FACING)));
//    }
//
//    @Override
//    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
//        builder.add(FACING);
//    }


    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level pLevel, BlockState pState, BlockEntityType<T> pBlockEntityType) {
        return pLevel.isClientSide() ? null : checkType(pBlockEntityType, ModBlockEntities.FLUID_BUYER.get(),
                (level, pos, state, blockEntity) -> FluidBuyerEntity.tick(level, pos, state, (FluidBuyerEntity) blockEntity));
    }

    private static <T extends BlockEntity> BlockEntityTicker<T> checkType(BlockEntityType<T> blockEntityType, BlockEntityType<?> expectedType, BlockEntityTicker<? super T> ticker) {
        return blockEntityType == expectedType ? (BlockEntityTicker<T>) ticker : null;
    }
}
