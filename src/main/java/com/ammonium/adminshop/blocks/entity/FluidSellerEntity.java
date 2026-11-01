package com.ammonium.adminshop.blocks.entity;

import com.ammonium.adminshop.AdminShop;
import com.ammonium.adminshop.blocks.interfaces.FluidSellerMachine;
import com.ammonium.adminshop.recipes.RecipeManager;
import com.ammonium.adminshop.recipes.SellFluidRecipe;
import com.ammonium.adminshop.screen.FluidSellerMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidStacksResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class FluidSellerEntity extends FluidHandlerBlockEntity implements FluidSellerMachine {
    public static final int TICK_COOLDOWN = 20;
    private static final int TANK_CAPACITY = 64000;

    private @Nullable UUID teamId = null;
    private @Nullable Identifier lockedRecipeId = null;
    private int tickCounter = 0; // unsynced
    private int tickProgress = 0; // synced

    public FluidSellerEntity(BlockPos pWorldPosition, BlockState pBlockState) {
        super(ModBlockEntities.FLUID_SELLER.get(), pWorldPosition, pBlockState);
        this.tank = new FluidStacksResourceHandler(1, TANK_CAPACITY) {
            @Override
            public boolean isValid(int index, FluidResource resource) {
                return level == null || RecipeManager.canPlaceFluidInSeller(level, resource.toStack(1), lockedRecipeId);
            }
        };
    }

    public int getTickCounter() {
        return this.tickCounter;
    }

    public void setTickCounter(int pTickCounter) {
        if (this.tickCounter == pTickCounter) { return; }
        int oldProgress = (this.tickCounter * 8) / TICK_COOLDOWN;
        int newProgress = (pTickCounter * 8) / TICK_COOLDOWN;
        this.tickCounter = pTickCounter;
        this.tickProgress = newProgress;
        if (oldProgress != newProgress) {
            this.setChanged();
            this.sendUpdates();
        }
    }

    public int getProgress() {
        // GetProgress returns the progress of the seller in the range of 0-8
        return this.tickProgress;
    }

    @Override
    public void setTeamId(UUID teamId) {
        this.teamId = teamId;
        this.setChanged();
        this.sendUpdates();
    }

    @Override
    public @Nullable UUID getTeamId() {
        return teamId;
    }

    public @Nullable Identifier getLockedRecipeId() {
        return lockedRecipeId;
    }

    public void setLockedRecipeId(@Nullable Identifier lockedRecipeId) {
        this.lockedRecipeId = lockedRecipeId;
        this.setChanged();
        this.sendUpdates();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.adminshop.fluid_seller");
    }

    @Override
    public void setChanged() {
        super.setChanged();
    }

    public FluidStacksResourceHandler getTank() {
        return this.tank;
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int pContainerId, Inventory pInventory, Player pPlayer) {
        return new FluidSellerMenu(pContainerId, pInventory, this);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, FluidSellerEntity sellerBE) {
        // Ignore if not server side
        if (level.isClientSide()) { return; }
        assert level instanceof ServerLevel;

        // Only run every 20 ticks
        int tickCounter = sellerBE.getTickCounter() + 1;
        if (tickCounter <= TICK_COOLDOWN) {
            sellerBE.setTickCounter(tickCounter);
            return;
        }
        sellerBE.setTickCounter(0);

        // Check for valid recipe
        FluidStack tankContents = FluidUtil.getStack(sellerBE.getTank(), 0);
        AdminShop.LOGGER.info("[FluidSeller] Tick — teamId={}, tank={} x{}mB", sellerBE.teamId,
                tankContents.isEmpty() ? "empty" : tankContents.getDescriptionId(), tankContents.getAmount());
        net.minecraft.world.item.crafting.RecipeHolder<SellFluidRecipe> recipeHolder = RecipeManager.checkForSellFluidRecipe((ServerLevel) level, sellerBE).orElse(null);
        if (recipeHolder == null) {
            AdminShop.LOGGER.info("[FluidSeller] No matching recipe found");
            return;
        }
        SellFluidRecipe recipe = recipeHolder.value();
        AdminShop.LOGGER.info("[FluidSeller] Matched recipe: price={}, fluid={} x{}mB", recipe.getPrice(),
                recipe.getFluid().getDescriptionId(), recipe.getFluid().getAmount());

        // Sell the fluid
        @Nullable IFluidHandler handler = IFluidHandler.of(sellerBE.getTank());
        if (handler == null) {
            AdminShop.LOGGER.info("[FluidSeller] Fluid handler is null");
            return;
        }

        handler.drain(recipe.getFluid().copy(), IFluidHandler.FluidAction.EXECUTE);
        recipe.sell((ServerLevel) level, sellerBE);
        sellerBE.setChanged();
        sellerBE.sendUpdates();
    }

    @Override
    public void onLoad() {
        super.onLoad();
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    public void sendUpdates() {
        if (this.level != null) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
        }
    }

    @Override
    protected void saveAdditional(@NotNull ValueOutput tag) {
        super.saveAdditional(tag);
        if (this.teamId != null) {
            tag.store("team", UUIDUtil.CODEC, this.teamId);
        }
        if (this.lockedRecipeId != null) {
            tag.putString("lockedRecipe", this.lockedRecipeId.toString());
        }
        tag.putInt("tickProgress", this.tickProgress);
    }

    @Override
    public void loadAdditional(@NotNull ValueInput tag) {
        super.loadAdditional(tag);
        tag.read("team", UUIDUtil.CODEC).ifPresent(team -> this.teamId = team);
        this.lockedRecipeId = tag.getString("lockedRecipe").map(Identifier::parse).orElse(null);
        this.tickProgress = tag.getIntOr("tickProgress", this.tickProgress);
    }

//    public void drops() {
//        SimpleContainer inventory = new SimpleContainer(itemHandler.getSlots());
//        for (int i = 0; i < itemHandler.getSlots(); i++) {
//            inventory.setItem(i, itemHandler.getStackInSlot(i));
//        }
//
//        Containers.dropContents(this.level, this.worldPosition, inventory);
//    }
}
