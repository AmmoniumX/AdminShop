package com.ammonium.adminshop.blocks;

import com.ammonium.adminshop.AdminShop;
import com.ammonium.adminshop.blocks.entity.AbstractBuyerEntity;
import com.ammonium.adminshop.money.MoneyHelper;
import com.ammonium.adminshop.screen.AbstractBuyerMenu;
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
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public abstract class AbstractBuyerBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    private static final VoxelShape RENDER_SHAPE = Shapes.box(0.1, 0.1, 0.1, 0.9, 0.9, 0.9);

    @FunctionalInterface
    public interface BlockEntityFactory<T extends AbstractBuyerEntity> {
        T createBlockEntity(BlockPos pos, BlockState state);
    }
    protected final BlockEntityFactory<?> BLOCK_ENTITY_FACTORY;

    @FunctionalInterface
    public interface MenuFactory<T extends AbstractBuyerMenu> {
        T createMenu(int id, Inventory inventory, @NotNull Level level, BlockPos pos);
    }
    protected final MenuFactory<?> MENU_FACTORY;

    public AbstractBuyerBlock( @NotNull BlockEntityFactory<?> blockEntityFactory,
                               @NotNull MenuFactory<?> menuFactory) {
        this(blockEntityFactory, menuFactory, defaultProperties());
    }

    protected AbstractBuyerBlock( @NotNull BlockEntityFactory<?> blockEntityFactory,
                               @NotNull MenuFactory<?> menuFactory,
                               BlockBehaviour.Properties properties) {
        super(properties);
        this.BLOCK_ENTITY_FACTORY = blockEntityFactory;
        this.MENU_FACTORY = menuFactory;
    }

    protected static BlockBehaviour.Properties defaultProperties() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.METAL)
                .sound(SoundType.METAL)
                .strength(1.0f)
                .lightLevel(state -> 0)
                .dynamicShape()
                .forceSolidOn()
                .noOcclusion()
                .pushReaction(PushReaction.BLOCK);
    }


    @Override
    public void onRemove(BlockState pState, Level pLevel, BlockPos pPos, BlockState pNewState, boolean pIsMoving) {
        if (pState.getBlock() != pNewState.getBlock()) {
            BlockEntity blockEntity = pLevel.getBlockEntity(pPos);
            if (blockEntity instanceof AbstractBuyerEntity buyerEntity) {
                buyerEntity.drops();
                buyerEntity.setRemoved();
            }
        }
        super.onRemove(pState, pLevel, pPos, pNewState, pIsMoving);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos,
                                 Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide()) {
            assert level instanceof ServerLevel;
            ServerLevel serverLevel = (ServerLevel) level;
            AdminShop.LOGGER.debug("Saving account");
            if(level.getBlockEntity(pos) instanceof AbstractBuyerEntity buyerEntity
                && player instanceof ServerPlayer serverPlayer) {
                @Nullable UUID teamId = buyerEntity.getTeamId();
                if (teamId == null) {
                    AdminShop.LOGGER.debug("Claiming unclaimed machine for {}", serverPlayer.getName().getString());
                    @Nullable UUID newTeamId = MoneyHelper.get(serverLevel).getTeamUUIDForPlayer(serverPlayer);
                    if (newTeamId == null) {
                        AdminShop.LOGGER.debug("Could not find FTB team for {}", serverPlayer.getName().getString());
                    } else {
                        buyerEntity.setTeamId(newTeamId);
                        player.playNotifySound(SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0f, 1.0f);
                        player.sendSystemMessage(Component.translatable("message.adminshop.claimed_machine"));
                        NetworkHooks.openScreen(serverPlayer, buyerEntity, pos);
                    }

                } else if (MoneyHelper.get(serverLevel).isMemberOfTeam(teamId, serverPlayer)) {
                    AdminShop.LOGGER.debug("Found account: {}", teamId);
                    // Open menu
                    NetworkHooks.openScreen((ServerPlayer) player, buyerEntity, pos);
                } else {
                    AdminShop.LOGGER.debug("Account not found");
                    // Wrong user
                    player.sendSystemMessage(Component.translatable("message.adminshop.no_access"));
                    AdminShop.LOGGER.debug("You don't have access to this machine's account!");
                }

            } else {
                throw new IllegalStateException("Our Container provider is missing!");
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }



    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return RENDER_SHAPE;
    }

    @Nullable
    @Override
    public MenuProvider getMenuProvider(BlockState state, Level level, BlockPos pos) {
        return new SimpleMenuProvider((id, inventory, player) ->
                MENU_FACTORY.createMenu(id, inventory, level, pos), Component.translatable("screen.adminshop.buyer"));
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return BLOCK_ENTITY_FACTORY.createBlockEntity(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide) {
            // Server side code
            ServerLevel serverLevel = (ServerLevel) level;
            BlockEntity blockEntity = level.getBlockEntity(pos);
            // Set initial values
            if (placer instanceof ServerPlayer serverPlayer && blockEntity instanceof AbstractBuyerEntity buyerEntity) {
                assignInitialTeamId(serverLevel, serverPlayer, buyerEntity);
            }
        }
    }

    /**
     * Hook so subclasses (e.g. the Creative Buyer) can leave the machine unclaimed when placed.
     */
    protected void assignInitialTeamId(ServerLevel serverLevel, ServerPlayer serverPlayer, AbstractBuyerEntity buyerEntity) {
        UUID teamId = MoneyHelper.get(serverLevel).getPlayerAccount(serverPlayer).teamId();
        AdminShop.LOGGER.debug("Setting initial teamId: {}", teamId);
        buyerEntity.setTeamId(teamId);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext pContext) {
        return this.defaultBlockState().setValue(FACING, pContext.getHorizontalDirection().getOpposite());
    }

    @Override
    public BlockState rotate(BlockState pState, Rotation pRotation) {
        return pState.setValue(FACING, pRotation.rotate(pState.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState pState, Mirror pMirror) {
        return pState.rotate(pMirror.getRotation(pState.getValue(FACING)));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }


//    @Nullable
//    @Override
//    public abstract <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level pLevel, BlockState pState, BlockEntityType<T> pBlockEntityType);
// //    {
// //        return pLevel.isClientSide() ? null : checkType(pBlockEntityType, ModBlockEntities.BUYER_1.get(),
// //                (level, pos, state, blockEntity) -> BuyerBE.tick(level, pos, state, (BuyerBE) blockEntity));
// //    }
//
//    protected static <T extends BlockEntity> BlockEntityTicker<T> checkType(BlockEntityType<T> blockEntityType, BlockEntityType<?> expectedType, BlockEntityTicker<? super T> ticker) {
//        return blockEntityType == expectedType ? (BlockEntityTicker<T>) ticker : null;
//    }

    /**
     * Gets the BlockEntityType for this block.
     * Each subclass must implement this to return its specific BlockEntityType.
     */
    protected abstract BlockEntityType<? extends AbstractBuyerEntity> getBlockEntityType();

    /**
     * Creates a ticker for the block entity.
     * Each subclass must implement this to return its specific ticker logic.
     */
    protected abstract <T extends AbstractBuyerEntity> BlockEntityTicker<T> createBlockEntityTicker();

    /**
     * Implementation of getTicker that uses the subclass-specific methods.
     * This avoids having to override getTicker in each subclass.
     */
    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level pLevel, BlockState pState, BlockEntityType<T> pBlockEntityType) {
        return pLevel.isClientSide() ? null :
                checkType(pBlockEntityType, getBlockEntityType(), createBlockEntityTicker());
    }

    /**
     * Helper method to safely cast the ticker if the types match.
     */
    @SuppressWarnings("unchecked")
    private static <T extends BlockEntity, E extends BlockEntity> BlockEntityTicker<T> checkType(
            BlockEntityType<T> actualType,
            BlockEntityType<E> expectedType,
            BlockEntityTicker<? super E> ticker) {
        return actualType == expectedType ? (BlockEntityTicker<T>) ticker : null;
    }
}
