package com.ammonium.adminshop.blocks;

import com.ammonium.adminshop.AdminShop;
import com.ammonium.adminshop.blocks.entity.AdvancedDetectorEntity;
import com.ammonium.adminshop.blocks.entity.ModBlockEntities;
import com.ammonium.adminshop.money.MoneyHelper;
import com.ammonium.adminshop.screen.AdvancedDetectorMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;

public class AdvancedDetector extends BaseEntityBlock {
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    private static final VoxelShape RENDER_SHAPE = Shapes.box(0, 0, 0, 1, 0.125, 1);
    public AdvancedDetector() {
        super(Properties.of(ModBlocks.machineBlock)
                .sound(SoundType.METAL)
                .strength(1.0f)
                .lightLevel(state -> 0)
                .dynamicShape()
                .noOcclusion()
        );
    }

    @Override
    public int getSignal(BlockState state, BlockGetter world, BlockPos pos, Direction direction) {
        if (direction.equals(Direction.UP)) return 0;
        BlockEntity be = world.getBlockEntity(pos);
        if (be instanceof AdvancedDetectorEntity advancedDetectorBE) {
            return advancedDetectorBE.getSignal();
        }
        return 0;
    }

    @Override
    public int getDirectSignal(BlockState state, BlockGetter world, BlockPos pos, Direction direction) {
        return getSignal(state, world, pos, direction);
    }

    @Override
    public boolean isSignalSource(BlockState state) {
        return true;
    }

    @Override
    public VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        return RENDER_SHAPE;
    }

    @Override
    public RenderShape getRenderShape(BlockState pState) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public MenuProvider getMenuProvider(BlockState pState, Level pLevel, BlockPos pPos) {
        return new SimpleMenuProvider((id, playerInventory, player) -> new AdvancedDetectorMenu(id, playerInventory, pLevel, pPos), Component.translatable("screen.adminshop.buyer"));
    }

    @Override
    public void setPlacedBy(Level pLevel, BlockPos pPos, BlockState pState, @Nullable LivingEntity pPlacer, ItemStack pStack) {
        super.setPlacedBy(pLevel, pPos, pState, pPlacer, pStack);
        if (!pLevel.isClientSide) {
            // Server side code
            ServerLevel serverLevel = (ServerLevel) pLevel;
            BlockEntity blockEntity = pLevel.getBlockEntity(pPos);
            // Set initial values
            if (pPlacer instanceof ServerPlayer serverPlayer && blockEntity instanceof AdvancedDetectorEntity advancedDetectorBE) {
                advancedDetectorBE.setTeamId(MoneyHelper.get(serverLevel).getPlayerAccount(serverPlayer).teamId());
            }
        }
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pPos, BlockState pState) {
        return new AdvancedDetectorEntity(pPos, pState);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext pContext) {
        return this.defaultBlockState().setValue(LIT, false);
    }

    @Override
    public InteractionResult use(BlockState pState, Level pLevel, BlockPos pPos,
                                 Player pPlayer, InteractionHand pHand, BlockHitResult pHit) {
        if (!pLevel.isClientSide()) {
            ServerLevel serverLevel = (ServerLevel) pLevel;
            if(pLevel.getBlockEntity(pPos) instanceof AdvancedDetectorEntity advancedDetectorBE
                    && pPlayer instanceof ServerPlayer serverPlayer) {

                if (MoneyHelper.get(serverLevel).isMemberOfTeam(advancedDetectorBE.getTeamId(), serverPlayer)) {
                    // Open menu
                    AdminShop.LOGGER.debug("Opening screen");
                    NetworkHooks.openScreen((ServerPlayer) pPlayer, advancedDetectorBE, pPos);
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

    @Override
    public void onRemove(BlockState pState, Level pLevel, BlockPos pPos, BlockState pNewState, boolean pIsMoving) {
        if (pState.getBlock() != pNewState.getBlock()) {
            BlockEntity blockEntity = pLevel.getBlockEntity(pPos);
            if (blockEntity instanceof AdvancedDetectorEntity advancedDetectorBE) {
                advancedDetectorBE.setRemoved();
            }
        }
        super.onRemove(pState, pLevel, pPos, pNewState, pIsMoving);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LIT);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level pLevel, BlockState pState, BlockEntityType<T> pBlockEntityType) {
        return pLevel.isClientSide() ? null : checkType(pBlockEntityType, ModBlockEntities.ADVANCED_DETECTOR.get(),
                (level, pos, state, blockEntity) -> AdvancedDetectorEntity.tick(level, pos, state, (AdvancedDetectorEntity) blockEntity));
    }

    private static <T extends BlockEntity> BlockEntityTicker<T> checkType(BlockEntityType<T> blockEntityType, BlockEntityType<?> expectedType, BlockEntityTicker<? super T> ticker) {
        return blockEntityType == expectedType ? (BlockEntityTicker<T>) ticker : null;
    }
}
