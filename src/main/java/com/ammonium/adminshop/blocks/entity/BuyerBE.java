package com.ammonium.adminshop.blocks.entity;

import com.ammonium.adminshop.AdminShop;
import com.ammonium.adminshop.blocks.ItemBuyerMachine;
import com.ammonium.adminshop.recipes.BuyItemRecipe;
import com.ammonium.adminshop.recipes.RecipeManager;
import com.ammonium.adminshop.screen.BuyerMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
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
import net.minecraftforge.items.ItemHandlerHelper;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;

public class BuyerBE extends BaseContainerBlockEntity implements ItemBuyerMachine, WorldlyContainer {
    private static final int slotSize = 1;

    private final NonNullList<ItemStack> stacks = NonNullList.withSize(slotSize, ItemStack.EMPTY);
    private final int[] slots = stacks.stream().mapToInt(stacks::indexOf).toArray();

    private String ownerUUID;
    private Pair<String, Integer> account;
    private ResourceLocation recipeId = null;
    private int tickCounter = 0;


    public BuyerBE(BlockPos blockPos, BlockState blockState) {
        super(ModBlockEntities.BUYER_1.get(), blockPos, blockState);
    }

    public void setOwnerUUID(String ownerUUID) {
        this.ownerUUID = ownerUUID;
        this.setChanged();
        this.sendUpdates();
    }

    public String getOwnerUUID() {
        return ownerUUID;
    }

    public void setAccount(Pair<String, Integer> account) {
        this.account = account;
        this.setChanged();
        this.sendUpdates();
    }

    public Pair<String, Integer> getAccountId() {
        return account;
    }
    public void setRecipe(ResourceLocation recipeId) {
        this.recipeId = recipeId;
        this.setChanged();
        this.sendUpdates();
    }

    public Optional<BuyItemRecipe> getRecipe(Level level) {
        return RecipeManager.getShopBuyItemRecipe(level, recipeId);
    }

    @Override
    public Component getDisplayName() {
        return Component.literal("Auto-Buyer");
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
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new BuyerMenu(id, inventory, this);
    }

    @Override
    protected AbstractContainerMenu createMenu(int id, Inventory inventory) {
        return new BuyerMenu(id, inventory, this);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, BuyerBE buyerBE) {
        // Ignore if not server side
        if (level.isClientSide) { return; }
        assert level instanceof ServerLevel;

        // Only run every 20 ticks
        buyerBE.tickCounter++;
        if (buyerBE.tickCounter <= 20) { return; }
        buyerBE.tickCounter = 0;

        // Check for valid recipe
        BuyItemRecipe recipe = buyerBE.getRecipe((ServerLevel) level).orElse(null);
        if (recipe == null) {
            AdminShop.LOGGER.debug("Buyer has no recipe");
            return;
        }
        boolean isValid = RecipeManager.checkForBuyItemRecipe((ServerLevel) level, buyerBE, recipe);
        if (!isValid) { return; }

        // Check for space
        IItemHandler handler = buyerBE.getCapability(ForgeCapabilities.ITEM_HANDLER).orElseThrow(NullPointerException::new);
        ItemStack simulated = ItemHandlerHelper.insertItemStacked(handler, recipe.getItem().copy(), true);
        if (!simulated.isEmpty()) {

            // Buy the item and add to inventory
            ItemStack item = recipe.buy((ServerLevel) level, buyerBE);
            assert item != null && !item.isEmpty();
            ItemHandlerHelper.insertItemStacked(handler, item, false);
            return;
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
        if (this.ownerUUID != null) {
            tag.putString("ownerUUID", this.ownerUUID);
        }
        if (this.account != null) {
            tag.putString("accountUUID", this.account.getKey());
            tag.putInt("accountID", this.account.getValue());
        }
        if (this.recipeId != null) {
            tag.putString("recipe", this.recipeId.toString());
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
        if (tag.contains("ownerUUID")) {
            this.ownerUUID = tag.getString("ownerUUID");
        }
        if (tag.contains("accountUUID") && tag.contains("accountID")) {
            String accountUUID = tag.getString("accountUUID");
            int accountID = tag.getInt("accountID");
            this.account = Pair.of(accountUUID, accountID);
        }
        if (tag.contains("recipe")) {
            this.recipeId = new ResourceLocation(tag.getString("recipe"));
        } else {
            AdminShop.LOGGER.debug("Buyer has no targetShopItem");
            this.recipeId = null;
        }
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag) {
        super.saveAdditional(tag);
        ContainerHelper.saveAllItems(tag, this.stacks);
        if (this.ownerUUID != null) {
            tag.putString("ownerUUID", this.ownerUUID);
        }
        if (this.account != null) {
            tag.putString("accountUUID", this.account.getKey());
            tag.putInt("accountID", this.account.getValue());
        }
        if (this.recipeId != null) {
            tag.putString("recipe", this.recipeId.toString());
        }
    }

    @Override
    public void load(@NotNull CompoundTag tag) {
        super.load(tag);
        ContainerHelper.loadAllItems(tag, this.stacks);
        if (tag.contains("ownerUUID")) {
            this.ownerUUID = tag.getString("ownerUUID");
        }
        if (tag.contains("accountUUID") && tag.contains("accountID")) {
            String accountUUID = tag.getString("accountUUID");
            int accountID = tag.getInt("accountID");
            this.account = Pair.of(accountUUID, accountID);
        }
        if (tag.contains("recipe")) {
            this.recipeId = new ResourceLocation(tag.getString("recipe"));
        } else {
            AdminShop.LOGGER.debug("Buyer has no targetShopItem");
            this.recipeId = null;
        }
    }

    public void drops() {
        Containers.dropContents(this.level, this.worldPosition, stacks);
    }

    @Override
    public int[] getSlotsForFace(Direction direction) {
        return slots;
    }

    // TODO: enable once we have recipe system fully working
//    @Override
//    public boolean canPlaceItem(int i, ItemStack itemStack) {
//        return false;
//    }

    @Override
    public boolean canPlaceItemThroughFace(int i, ItemStack itemStack, @Nullable Direction direction) {
        return this.canPlaceItem(i, itemStack);
    }

    @Override
    public boolean canTakeItemThroughFace(int i, ItemStack itemStack, Direction direction) {
        return true;
    }

    @Override
    public void clearContent() {
        this.stacks.replaceAll(ignored -> ItemStack.EMPTY);
    }
}
