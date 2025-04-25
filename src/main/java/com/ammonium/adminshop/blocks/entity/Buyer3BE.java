package com.ammonium.adminshop.blocks.entity;

import com.ammonium.adminshop.AdminShop;
import com.ammonium.adminshop.blocks.ItemBuyerMachine;
import com.ammonium.adminshop.blocks.ShopMachine;
import com.ammonium.adminshop.money.BankAccount;
import com.ammonium.adminshop.money.MoneyManager;
import com.ammonium.adminshop.network.PacketSyncMoneyToClient;
import com.ammonium.adminshop.screen.Buyer3Menu;
import com.ammonium.adminshop.setup.Messages;
import com.ammonium.adminshop.shop.Shop;
import com.ammonium.adminshop.shop.ShopItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static java.lang.Math.ceil;

public class Buyer3BE extends BlockEntity implements ItemBuyerMachine, ShopMachine {
    private String ownerUUID;
    private Pair<String, Integer> account;
    private boolean hasNBT = false;
    private ShopItem targetShopItem = null;
    private int tickCounter = 0;

    private final int buySize = 64;
    private final int slotSize = 5;

    private final ItemStackHandler itemHandler = new ItemStackHandler(slotSize) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };
    public ItemStackHandler getItemHandler() {
        return itemHandler;
    }

    private LazyOptional<IItemHandler> lazyItemHandler = LazyOptional.empty();

    public Buyer3BE(BlockPos pWorldPosition, BlockState pBlockState) {
        super(ModBlockEntities.BUYER_3.get(), pWorldPosition, pBlockState);
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

    public Pair<String, Integer> getAccount() {
        return account;
    }

    public void setTargetShopItem(ShopItem item) {
        this.targetShopItem = item;
        if(item != null) this.hasNBT = item.hasNBT();
        this.setChanged();
        this.sendUpdates();
    }

    public ShopItem getTargetShopItem() {
        return this.targetShopItem;
    }

    @Override
    public Component getDisplayName() {
        return Component.literal("Auto-Buyer");
    }

    @Override
    public void setChanged() {
        super.setChanged();
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int pContainerId, Inventory pInventory, Player pPlayer) {
        return new Buyer3Menu(pContainerId, pInventory, this);
    }

    public static void tick(Level pLevel, BlockPos pPos, BlockState pState, Buyer3BE pBlockEntity) {
        if(!pLevel.isClientSide) {
            pBlockEntity.tickCounter++;
            if (pBlockEntity.tickCounter > 20) {
                pBlockEntity.tickCounter = 0;
                // Send buy item transaction (send pos and buySize)
                assert pLevel instanceof ServerLevel;
                buyerTransaction(pPos, (ServerLevel) pLevel, pBlockEntity, pBlockEntity.buySize);
            }
        }
    }

    public static void buyerTransaction(BlockPos pos, ServerLevel level, Buyer3BE buyerEntity, int buySize) {
//        System.out.println("Processing buyer transaction for "+pos+", "+buySize);
        // item logic
        // Attempt to insert the items, and only perform transaction on what can fit
        MoneyManager moneyManager = MoneyManager.get(level);
        // Check shopBuyIndex
        if (buyerEntity.targetShopItem == null) {
            return;
        }

        ShopItem shopItem = buyerEntity.getTargetShopItem();
        // Check shopItem is item and buy only
        if (!shopItem.isBuy() || !shopItem.isItem()) {
            AdminShop.LOGGER.debug("Buyer shopItem is not buy item!");
            return;
        }
        if (shopItem.getItem().isEmpty()) {
            AdminShop.LOGGER.debug("Buyer shopItem is empty!");
            return;
        }

        ItemStack toInsert = shopItem.getItem().copy();
        toInsert.setCount(buySize);
        ItemStackHandler handler = buyerEntity.getItemHandler();
        ItemStack returned = ItemHandlerHelper.insertItemStacked(handler, toInsert, true);
        if(returned.getCount() == buySize) {
            return;
        }
        long itemCost = shopItem.getPrice();
        long price = (long) ceil((buySize - returned.getCount()) * itemCost);
        // Get MoneyManager and attempt transaction

        // Check if account is set
        if (buyerEntity.account == null) {
            AdminShop.LOGGER.debug("Buyer bankAccount is null");
            return;
        }

        // Check if account still exists
        if (!moneyManager.existsBankAccount(buyerEntity.account)) {
            AdminShop.LOGGER.debug("Buyer machine account "+buyerEntity.account.getKey()+":"+buyerEntity.account
                    .getValue()+" does not exist");
            return;
        }
        String accOwner = buyerEntity.account.getKey();
        int accID = buyerEntity.account.getValue();
        // Check if account has enough money, if not reduce amount
        long balance = moneyManager.getBalance(accOwner, accID);
        if (price > balance) {
            if (itemCost > balance) {
                // not enough money to buy one
                return;
            }
            // Find max amount he can buy
            buySize = Math.min((int) (balance / itemCost), buySize);
            price = (long) ceil(buySize * itemCost);
            toInsert.setCount(buySize);
        }
        boolean success = moneyManager.subtractBalance(accOwner, accID, price);
        if (success) {
            ItemHandlerHelper.insertItemStacked(handler, toInsert, false);
//            System.out.println("Bought item");
        } else {
            AdminShop.LOGGER.debug("Error selling item.");
            return;
        }
        // Sync account data
        AdminShop.LOGGER.debug("Syncing money with clients");
        // Get current bank account
        BankAccount currentAccount = moneyManager.getBankAccount(accOwner, accID);

        // Sync money with bank account's members
        assert currentAccount.getMembers().contains(accOwner);
        currentAccount.getMembers().forEach(memberUUID -> {
            List<BankAccount> usableAccounts = moneyManager.getSharedAccounts().get(memberUUID);
            ServerPlayer playerByUUID = (ServerPlayer) level.getPlayerByUUID(UUID.fromString(memberUUID));
            if (playerByUUID == null) return;
//            AdminShop.LOGGER.debug("Syncing money with "+playerByUUID.getName().getString());
            Messages.sendToPlayer(new PacketSyncMoneyToClient(usableAccounts), playerByUUID);

        });
    }

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @javax.annotation.Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            return lazyItemHandler.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        lazyItemHandler = LazyOptional.of(() -> itemHandler);
    }

    @Override
    public void invalidateCaps()  {
        super.invalidateCaps();
        lazyItemHandler.invalidate();
    }

    @Override
    public @NotNull CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        tag.put("inventory", this.itemHandler.serializeNBT());
        if (this.ownerUUID != null) {
            tag.putString("ownerUUID", this.ownerUUID);
        }
        if (this.account != null) {
            tag.putString("accountUUID", this.account.getKey());
            tag.putInt("accountID", this.account.getValue());
        }
        if (this.targetShopItem != null) {
            ResourceLocation targetResource = ForgeRegistries.ITEMS.getKey(this.targetShopItem.getItem().getItem());
            assert targetResource != null;
            tag.putString("targetResource", targetResource.toString());
            tag.putBoolean("hasNBT", this.hasNBT);
            if (this.hasNBT) {
                // If NBT item, save index of List<Item>
                tag.putInt("indexTargetNBT", Shop.get().getShopStockBuyNBT().get(this.targetShopItem.getItem().getItem()).indexOf(this.targetShopItem));
            }
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
        this.itemHandler.deserializeNBT(tag.getCompound("inventory"));
        if (tag.contains("ownerUUID")) {
            this.ownerUUID = tag.getString("ownerUUID");
        }
        if (tag.contains("accountUUID") && tag.contains("accountID")) {
            String accountUUID = tag.getString("accountUUID");
            int accountID = tag.getInt("accountID");
            this.account = Pair.of(accountUUID, accountID);
        }
        ResourceLocation targetResource = null;
        if (tag.contains("targetResource")) {
            targetResource = new ResourceLocation(tag.getString("targetResource"));
        }
        if (tag.contains("hasNBT")) {
            this.hasNBT = tag.getBoolean("hasNBT");
        }
        if (targetResource == null) {
            AdminShop.LOGGER.debug("Buyer has no targetShopItem");
            this.targetShopItem = null;
        } else {
            Item targetItem = ForgeRegistries.ITEMS.getValue(targetResource);
            if (!this.hasNBT) {
                this.targetShopItem = Shop.get().getBuyShopItem(targetItem);
            } else if (tag.contains("indexTargetNBT")){
                int indexTargetNBT = tag.getInt("indexTargetNBT");
                this.targetShopItem = Shop.get().getShopStockBuyNBT().get(targetItem).get(indexTargetNBT);
            } else {
                AdminShop.LOGGER.error("Buyer target has hasNBT but no indexTargetNBT!");
                this.targetShopItem = null;
            }
        }
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("inventory", this.itemHandler.serializeNBT());
        if (this.ownerUUID != null) {
            tag.putString("ownerUUID", this.ownerUUID);
        }
        if (this.account != null) {
            tag.putString("accountUUID", this.account.getKey());
            tag.putInt("accountID", this.account.getValue());
        }
        if (this.targetShopItem != null) {
            ResourceLocation targetResource = ForgeRegistries.ITEMS.getKey(this.targetShopItem.getItem().getItem());
            assert targetResource != null;
            tag.putString("targetResource", targetResource.toString());
            tag.putBoolean("hasNBT", this.hasNBT);
            if (this.hasNBT) {
                // If NBT item, save index of List<Item>
                tag.putInt("indexTargetNBT", Shop.get().getShopStockBuyNBT().get(this.targetShopItem.getItem().getItem()).indexOf(this.targetShopItem));
            }
        }
    }

    @Override
    public void load(@NotNull CompoundTag tag) {
        super.load(tag);
        this.itemHandler.deserializeNBT(tag.getCompound("inventory"));
        if (tag.contains("ownerUUID")) {
            this.ownerUUID = tag.getString("ownerUUID");
        }
        if (tag.contains("accountUUID") && tag.contains("accountID")) {
            String accountUUID = tag.getString("accountUUID");
            int accountID = tag.getInt("accountID");
            this.account = Pair.of(accountUUID, accountID);
        }
        ResourceLocation targetResource = null;
        if (tag.contains("targetResource")) {
            targetResource = new ResourceLocation(tag.getString("targetResource"));
        }
        if (tag.contains("hasNBT")) {
            this.hasNBT = tag.getBoolean("hasNBT");
        }
        if (targetResource == null) {
            AdminShop.LOGGER.debug("Buyer has no targetShopItem");
            this.targetShopItem = null;
        } else {
            Item targetItem = ForgeRegistries.ITEMS.getValue(targetResource);
            if (!this.hasNBT) {
                this.targetShopItem = Shop.get().getBuyShopItem(targetItem);
            } else if (tag.contains("indexTargetNBT")){
                int indexTargetNBT = tag.getInt("indexTargetNBT");
                this.targetShopItem = Shop.get().getShopStockBuyNBT().get(targetItem).get(indexTargetNBT);
            } else {
                AdminShop.LOGGER.debug("Buyer target has hasNBT but no indexTargetNBT!");
                this.targetShopItem = null;
            }
            AdminShop.LOGGER.debug("Loaded buyer with targetShopItem "+((this.targetShopItem != null) ? this.targetShopItem.getItem().getDisplayName().getString() : "none"));
        }
    }

    public void drops() {
        SimpleContainer inventory = new SimpleContainer(itemHandler.getSlots());
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            inventory.setItem(i, itemHandler.getStackInSlot(i));
        }

        Containers.dropContents(this.level, this.worldPosition, inventory);
    }
}
