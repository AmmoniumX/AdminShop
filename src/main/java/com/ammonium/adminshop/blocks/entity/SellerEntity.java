package com.ammonium.adminshop.blocks.entity;

import com.ammonium.adminshop.AdminShop;
import com.ammonium.adminshop.blocks.interfaces.ItemSellerMachine;
import com.ammonium.adminshop.recipes.RecipeManager;
import com.ammonium.adminshop.recipes.SellItemRecipe;
import com.ammonium.adminshop.screen.SellerMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class SellerEntity extends BlockEntity implements ItemSellerMachine {
    private static final int SLOT_SIZE = 1;
    public static final int TICK_COOLDOWN = 20;
//    private final NonNullList<ItemStack> stacks = NonNullList.withSize(slotSize, ItemStack.EMPTY);
//    private final int[] slots = stacks.stream().mapToInt(stacks::indexOf).toArray();

    public final ItemStackHandler inventory = new ItemStackHandler(SLOT_SIZE) {
        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            // 1. Basic safety check (optional, ItemStackHandler handles nulls/empty)
            if (stack.isEmpty()) return false;

            // 2. Your custom logic
            Level level = getLevel(); // BlockEntity method to get current level
            if (level == null) {
                return false;
            }

            // Return your RecipeManager result
            return RecipeManager.canPlaceItemInSeller(level, stack);
        }

        @Override
        protected void onContentsChanged(int slot) {
            super.onContentsChanged(slot);
            setChanged();
            sendUpdates();
        }
    };

    public ItemStackHandler getInventory() {
        return this.inventory;
    }

    @Override
    public IItemHandler getItemHandler() {
        return this.inventory;
    }

    private UUID teamId = null;
    private int tickCounter = 0;    // unsynced
    private int tickProgress = 0;   // synced

    public SellerEntity(BlockPos pWorldPosition, BlockState pBlockState) {
        super(ModBlockEntities.SELLER.get(), pWorldPosition, pBlockState);
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
    public UUID getTeamId() {
        return teamId;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.adminshop.seller");
    }

//    @Override
//    protected Component getDefaultName() {
//        return getDisplayName();
//    }

//    @Override
//    public int getContainerSize() {
//        return SLOT_SIZE;
//    }

//    @Override
//    public boolean isEmpty() {
//        return java.util.stream.IntStream.range(0, SLOT_SIZE)
//                .allMatch(i -> inventory.getStackInSlot(i).isEmpty());
//    }

    @Override
    public void setChanged() {
        super.setChanged();
    }

//    @Override
//    public boolean stillValid(Player player) {
//        return true;
//    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int pContainerId, Inventory pInventory, Player pPlayer) {
        return new SellerMenu(pContainerId, pInventory, this);
    }

//    @Override
//    protected AbstractContainerMenu createMenu(int i, Inventory inventory) {
//        return new SellerMenu(i, inventory, this);
//    }

    public static void tick(Level level, BlockPos pos, BlockState state, SellerEntity sellerBE) {

        // Ignore if not server side
        if (level.isClientSide) { return; }
        assert level instanceof ServerLevel;

        // Only run every 20 ticks
        int tickCounter = sellerBE.getTickCounter() + 1;
        if (tickCounter <= TICK_COOLDOWN) {
            sellerBE.setTickCounter(tickCounter);
            return;
        }
        sellerBE.setTickCounter(0);

        // Check for valid recipe
        AdminShop.LOGGER.info("[Seller] Tick — teamId={}, slot0={}", sellerBE.teamId,
                sellerBE.inventory.getStackInSlot(0).isEmpty() ? "empty" : sellerBE.inventory.getStackInSlot(0).getDisplayName().getString());
        net.minecraft.world.item.crafting.RecipeHolder<SellItemRecipe> recipeHolder =
                RecipeManager.checkForSellItemRecipe((ServerLevel) level, sellerBE).orElse(null);
        if (recipeHolder == null) {
            AdminShop.LOGGER.info("[Seller] No matching recipe found");
            return;
        }
        SellItemRecipe recipe = recipeHolder.value();
        AdminShop.LOGGER.info("[Seller] Matched recipe: {}", recipeHolder.id());

        // Sell the item
        @Nullable IItemHandler handler = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, state, sellerBE, null);
        if (handler == null) {
            AdminShop.LOGGER.info("[Seller] Handler is null — capability not registered?");
            return;
        }
        AdminShop.LOGGER.info("[Seller] Handler slots={}", handler.getSlots());
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            ItemStack slotStack = handler.getStackInSlot(slot);
            AdminShop.LOGGER.info("[Seller] Slot {}: {} x{}", slot,
                    slotStack.isEmpty() ? "empty" : slotStack.getDisplayName().getString(), slotStack.getCount());
            if (!recipe.isMatchingItemStack(slotStack)) {
                AdminShop.LOGGER.info("[Seller] Slot {} doesn't match recipe", slot);
                continue;
            }
            ItemStack simulatedResult = handler.extractItem(slot, recipe.getCount(), true);
            AdminShop.LOGGER.info("[Seller] Simulate extract {}: got {}", recipe.getCount(), simulatedResult.getCount());
            if (!simulatedResult.isEmpty() && simulatedResult.getCount() == recipe.getCount()) {
                ItemStack itemResult = handler.extractItem(slot, recipe.getCount(), false);
                recipe.sell((ServerLevel) level, sellerBE);
                AdminShop.LOGGER.info("[Seller] Sold {} x{}", itemResult.getDisplayName().getString(), itemResult.getCount());
                return;
            }
        }
        AdminShop.LOGGER.info("[Seller] No slot had enough items to sell");
    }

    @Override
    public void onLoad() {
        super.onLoad();
    }

//    @Override
//    public void invalidateCaps()  {
//        super.invalidateCaps();
//    }

    @Override
    public @NotNull CompoundTag getUpdateTag(HolderLookup.Provider provider) {
        CompoundTag tag = super.getUpdateTag(provider);

        tag.put("Inventory", this.inventory.serializeNBT(provider));

        if (this.teamId != null) {
            tag.putUUID("team", this.teamId);
        }
        tag.putInt("tickProgress", this.tickProgress);
        return tag;
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
    protected void saveAdditional(@NotNull CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
//        ContainerHelper.saveAllItems(tag, this.inventory);

        tag.put("Inventory", this.inventory.serializeNBT(provider));

        if (this.teamId != null) {
            tag.putUUID("team", this.teamId);
        }
        tag.putInt("tickProgress", this.tickProgress);
    }

    @Override
    public void loadAdditional(@NotNull CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
//        ContainerHelper.loadAllItems(tag, this.inventory);

        if (tag.contains("Inventory")) {
            this.inventory.deserializeNBT(provider, tag.getCompound("Inventory"));
        }

        if (tag.contains("team")) {
            this.teamId = tag.getUUID("team");
        }
        if (tag.contains("tickProgress")) {
            this.tickProgress = tag.getInt("tickProgress");
        }
    }

    public void drops() {
//        Containers.dropContents(this.level, this.worldPosition, inventory);
        if (this.level == null) return;

        for (int i = 0; i < inventory.getSlots(); i++) {
            // Use the handler to get the stacks
            Containers.dropItemStack(this.level, this.worldPosition.getX(), this.worldPosition.getY(), this.worldPosition.getZ(), inventory.getStackInSlot(i));
        }
    }

//    @Override
//    public int[] getSlotsForFace(Direction direction) {
//        return slots;
//    }

//    @Override
//    public boolean canPlaceItem(int i, ItemStack itemStack) {
//        boolean fits = super.canPlaceItem(i, itemStack);
//        if (!fits) { return false; }
//        Level level = this.level;
//        if (level == null) {
//            AdminShop.LOGGER.debug("Level is null");
//            return false;
//        }
//        return RecipeManager.canPlaceItemInSeller(level, itemStack);
//    }

//    @Override
//    public boolean canPlaceItemThroughFace(int i, ItemStack itemStack, @Nullable Direction direction) {
//        return this.canPlaceItem(i, itemStack);
//    }

    // TODO: enable once we have recipe system fully working
//    @Override
//    public boolean canTakeItemThroughFace(int i, ItemStack itemStack, Direction direction) {
//        return false;
//    }

//    @Override
//    public boolean canTakeItemThroughFace(int i, ItemStack itemStack, Direction direction) {
//        return true;
//    }

//    @Override
//    public void clearContent() {
////        this.inventory.replaceAll(ignored -> ItemStack.EMPTY);
//        for (int i = 0; i < this.inventory.getSlots(); i++) {
//            this.inventory.setStackInSlot(i, ItemStack.EMPTY);
//        }
//    }
}
