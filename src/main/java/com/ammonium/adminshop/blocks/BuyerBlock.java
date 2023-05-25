package com.ammonium.adminshop.blocks;

import com.ammonium.adminshop.blocks.entity.BuyerBE;
import com.ammonium.adminshop.blocks.entity.ModBlockEntities;
import com.ammonium.adminshop.money.BuyerTargetInfo;
import com.ammonium.adminshop.money.ClientLocalData;
import com.ammonium.adminshop.money.MachineOwnerInfo;
import com.ammonium.adminshop.network.PacketBuyerInfoRequest;
import com.ammonium.adminshop.network.PacketSetMachineInfo;
import com.ammonium.adminshop.screen.BuyerMenu;
import com.ammonium.adminshop.setup.Messages;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class BuyerBlock extends CustomDirectionalBlock implements EntityBlock {
    public BuyerBlock() {
        super(Properties.of(Material.METAL)
                .sound(SoundType.METAL)
                .strength(1.0f)
                .lightLevel(state -> 0)
                .dynamicShape()
                .noOcclusion()
        );
    }
    private static final VoxelShape RENDER_SHAPE = Shapes.box(0.1, 0.1, 0.1, 0.9, 0.9, 0.9);


    @Override
    public void onRemove(BlockState pState, Level pLevel, BlockPos pPos, BlockState pNewState, boolean pIsMoving) {
        if (pState.getBlock() != pNewState.getBlock()) {
            BlockEntity blockEntity = pLevel.getBlockEntity(pPos);
            if (blockEntity instanceof BuyerBE buyerEntity) {
                buyerEntity.drops();
                buyerEntity.setRemoved();
                if (pLevel.isClientSide) {
                    System.out.println("Remove from client side");
                    ClientLocalData.removeMachineInfo(pPos);
                    ClientLocalData.removeBuyerTarget(pPos);
                } else {
                    System.out.println("Remove from server side");
                    MachineOwnerInfo.get(pLevel).removeMachineInfo(pPos);
                    BuyerTargetInfo.get(pLevel).removeBuyerTarget(pPos);
                    ClientLocalData.removeMachineInfo(pPos);
                    ClientLocalData.removeBuyerTarget(pPos);
                }
            }
        }
        super.onRemove(pState, pLevel, pPos, pNewState, pIsMoving);
    }

    @Override
    public InteractionResult use(BlockState pState, Level pLevel, BlockPos pPos,
                                 Player pPlayer, InteractionHand pHand, BlockHitResult pHit) {
        if (pLevel.isClientSide()) {
            if(pLevel.getBlockEntity(pPos) instanceof BuyerBE) {
                // Send the request packet
                Messages.sendToServer(new PacketBuyerInfoRequest(pPos));
            } else {
                throw new IllegalStateException("Our Container provider is missing!");
            }
        }
        return InteractionResult.sidedSuccess(pLevel.isClientSide());
    }



    @Override
    public VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        return RENDER_SHAPE;
    }

    @Nullable
    @Override
    public MenuProvider getMenuProvider(BlockState pState, Level pLevel, BlockPos pPos) {
        return new SimpleMenuProvider((id, playerInventory, player) -> {
            return new BuyerMenu(id, playerInventory, pLevel, pPos);
        }, new TranslatableComponent("screen.adminshop.buyer"));
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pPos, BlockState pState) {
        return new BuyerBE(pPos, pState);
    }

    @Override
    public RenderShape getRenderShape(BlockState pState) {
        return RenderShape.MODEL;
    }

    @Override
    public void setPlacedBy(Level pLevel, BlockPos pPos, BlockState pState, @Nullable LivingEntity pPlacer, ItemStack pStack) {
        super.setPlacedBy(pLevel, pPos, pState, pPlacer, pStack);
        if (pLevel.isClientSide) {
            BlockEntity blockEntity = pLevel.getBlockEntity(pPos);
            assert (pPlacer instanceof Player && blockEntity instanceof BuyerBE);
            // Set account info
//            ((BuyerBE) blockEntity).setAccInfo(pPlacer.getStringUUID(), pPlacer.getStringUUID(), 1);
            Messages.sendToServer(new PacketSetMachineInfo(pPlacer.getStringUUID(), pPlacer.getStringUUID(), 1, pPos));
        }
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level pLevel, BlockState pState, BlockEntityType<T> pBlockEntityType) {
        return pLevel.isClientSide() ? null : checkType(pBlockEntityType, ModBlockEntities.BUYER_1.get(),
                (level, pos, state, blockEntity) -> BuyerBE.tick(level, pos, state, (BuyerBE) blockEntity));
    }

    private static <T extends BlockEntity> BlockEntityTicker<T> checkType(BlockEntityType<T> blockEntityType, BlockEntityType<?> expectedType, BlockEntityTicker<? super T> ticker) {
        return blockEntityType == expectedType ? (BlockEntityTicker<T>) ticker : null;
    }
}
