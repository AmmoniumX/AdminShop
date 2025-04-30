package com.ammonium.adminshop.blocks.entity;

import com.ammonium.adminshop.AdminShop;
import com.ammonium.adminshop.blocks.interfaces.ItemSellerMachine;
import com.ammonium.adminshop.recipes.RecipeManager;
import com.ammonium.adminshop.recipes.SellItemRecipe;
import com.ammonium.adminshop.screen.SellerMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.UUID;

public class SellerBE extends BaseContainerBlockEntity implements ItemSellerMachine, WorldlyContainer {
    private static final int slotSize = 1;

    private final NonNullList<ItemStack> stacks = NonNullList.withSize(slotSize, ItemStack.EMPTY);
    private final int[] slots = stacks.stream().mapToInt(stacks::indexOf).toArray();

    private UUID teamId = null;
    private int tickCounter = 0;

    public SellerBE(BlockPos pWorldPosition, BlockState pBlockState) {
        super(ModBlockEntities.SELLER.get(), pWorldPosition, pBlockState);
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
        return Component.literal("Auto-Seller");
    }

    @Override
    protected Component getDefaultName() {
        return getDisplayName();
    }

    @Override
    public int getContainerSize() {
        return slotSize;
    }

    @Override
    public boolean isEmpty() {
        return this.stacks.stream().allMatch(ItemStack::isEmpty);
    }

    @Override
    public ItemStack getItem(int i) {
        return this.stacks.get(i);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack stack = ContainerHelper.removeItem(stacks, slot, amount);
        if (!stack.isEmpty()) {
            this.setChanged();
        }
        this.sendUpdates();
        return stack;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack stack = ContainerHelper.takeItem(stacks, slot);
        this.sendUpdates();
        return stack;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        stacks.set(slot, stack);
        this.sendUpdates();
    }

    @Override
    public void setChanged() {
        super.setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int pContainerId, Inventory pInventory, Player pPlayer) {
        return new SellerMenu(pContainerId, pInventory, this);
    }

    @Override
    protected AbstractContainerMenu createMenu(int i, Inventory inventory) {
        return new SellerMenu(i, inventory, this);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, SellerBE sellerBE) {
        // Ignore if not server side
        if (level.isClientSide) { return; }
        assert level instanceof ServerLevel;

        // Only run every 20 ticks
        sellerBE.tickCounter++;
        if (sellerBE.tickCounter <= 20) { return; }
        sellerBE.tickCounter = 0;

        // Check for valid recipe
        SellItemRecipe recipe = RecipeManager.checkForSellItemRecipe((ServerLevel) level, sellerBE).orElse(null);
        if (recipe == null) { return; }

        // Sell the item
        IItemHandler handler = sellerBE.getCapability(ForgeCapabilities.ITEM_HANDLER).orElse(null);
        if (handler == null) {
            AdminShop.LOGGER.debug("Handler is null");
            return;
        }
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            if (!recipe.isMatchingItemStack(handler.getStackInSlot(slot))) { continue; }
            ItemStack simulatedResult = handler.extractItem(slot, recipe.getCount(), true);
            if (!simulatedResult.isEmpty() && simulatedResult.getCount() == recipe.getCount()) {
                ItemStack itemResult = handler.extractItem(slot, recipe.getCount(), false);
                recipe.sell((ServerLevel) level, sellerBE);
                AdminShop.LOGGER.debug("Sold item: {}", itemResult);
                return;
            }
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
    }

    @Override
    public void invalidateCaps()  {
        super.invalidateCaps();
    }
    @Override
    public @NotNull CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        ContainerHelper.saveAllItems(tag, this.stacks);
        if (this.teamId != null) {
            tag.putUUID("team", this.teamId);
        }
        return tag;
    }
    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt) {
        super.onDataPacket(net, pkt);
        this.load(Objects.requireNonNull(pkt.getTag()));
    }

    public void sendUpdates() {
        if (this.level != null) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
        }
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        super.handleUpdateTag(tag);
        ContainerHelper.loadAllItems(tag, this.stacks);
        if (tag.contains("team")) {
            this.teamId = tag.getUUID("team");
        }
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag) {
        super.saveAdditional(tag);
        ContainerHelper.saveAllItems(tag, this.stacks);
        if (this.teamId != null) {
            tag.putUUID("team", this.teamId);
        }
    }

    @Override
    public void load(@NotNull CompoundTag tag) {
        super.load(tag);
        ContainerHelper.loadAllItems(tag, this.stacks);
        if (tag.contains("team")) {
            this.teamId = tag.getUUID("team");
        }
    }

    public void drops() {
        Containers.dropContents(this.level, this.worldPosition, stacks);
    }

    @Override
    public int[] getSlotsForFace(Direction direction) {
        return slots;
    }

    @Override
    public boolean canPlaceItem(int i, ItemStack itemStack) {
        boolean fits = super.canPlaceItem(i, itemStack);
        if (!fits) { return false; }
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            AdminShop.LOGGER.debug("Level is null");
            return false;
        }
        return RecipeManager.canPlaceItemInSeller(level, itemStack);
    }

    @Override
    public boolean canPlaceItemThroughFace(int i, ItemStack itemStack, @Nullable Direction direction) {
        return this.canPlaceItem(i, itemStack);
    }

    // TODO: enable once we have recipe system fully working
//    @Override
//    public boolean canTakeItemThroughFace(int i, ItemStack itemStack, Direction direction) {
//        return false;
//    }

    @Override
    public boolean canTakeItemThroughFace(int i, ItemStack itemStack, Direction direction) {
        return true;
    }

    @Override
    public void clearContent() {
        this.stacks.replaceAll(ignored -> ItemStack.EMPTY);
    }
}
