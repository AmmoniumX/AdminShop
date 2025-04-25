package com.ammonium.adminshop.blocks.entity;

import com.ammonium.adminshop.AdminShop;
import com.ammonium.adminshop.blocks.ShopMachine;
import com.ammonium.adminshop.money.BankAccount;
import com.ammonium.adminshop.money.MoneyManager;
import com.ammonium.adminshop.network.PacketSyncMoneyToClient;
import com.ammonium.adminshop.screen.SellerMenu;
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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
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
import net.minecraftforge.items.ItemStackHandler;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class SellerBE extends BlockEntity implements ShopMachine {
    private String ownerUUID;
    private Pair<String, Integer> account;

    private int tickCounter = 0;
    private final ItemStackHandler itemHandler = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            // Check if item is in item map
            boolean result = Shop.get().getShopSellItemMap().containsKey(stack.getItem());
            if (!result) {
                // Check if item tags are in item tags map
                Optional<TagKey<Item>> searchTag = stack.getTags().filter(itemTag -> Shop.get().hasSellShopItemTag(itemTag)).findFirst();
                result = searchTag.isPresent();
            }
            if (result) {
                return super.isItemValid(slot, stack);
            } else {
                return false;
            }
        }
    };

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

    public final ItemStackHandler getItemHandler() {
        return itemHandler;
    }

    private LazyOptional<IItemHandler> lazyItemHandler = LazyOptional.empty();

    public SellerBE(BlockPos pWorldPosition, BlockState pBlockState) {
        super(ModBlockEntities.SELLER.get(), pWorldPosition, pBlockState);
    }

    @Override
    public Component getDisplayName() {
        return Component.literal("Auto-Seller");
    }

    @Override
    public void setChanged() {
        super.setChanged();
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int pContainerId, Inventory pInventory, Player pPlayer) {
        return new SellerMenu(pContainerId, pInventory, this);
    }

    public static void tick(Level pLevel, BlockPos pPos, BlockState pState, SellerBE pBlockEntity) {
        if(hasItem(pBlockEntity)) {
            pBlockEntity.tickCounter++;
            if (pBlockEntity.tickCounter > 20) {
                pBlockEntity.tickCounter = 0;
                // Send sell transaction
                if (!pLevel.isClientSide) {
                    assert pLevel instanceof ServerLevel;
                    sellerTransaction(pPos, pBlockEntity, (ServerLevel) pLevel);
                }
            }
        }
    }

    public static void sellerTransaction(BlockPos pos, SellerBE sellerEntity, ServerLevel level) {
        ItemStackHandler itemHandler = sellerEntity.getItemHandler();
        ItemStack toSell = itemHandler.getStackInSlot(0);
        int count = toSell.getCount();
        boolean isShopItem = Shop.get().hasSellShopItem(toSell.getItem());
        ShopItem shopItem;
        if (isShopItem) {
            shopItem = Shop.get().getSellShopItem(toSell.getItem());
        } else {
            // Check if item tags are in item tags map
            Optional<TagKey<Item>> searchTag = toSell.getTags().filter(itemTag -> Shop.get().hasSellShopItemTag(itemTag)).findFirst();
            shopItem = searchTag.map(itemTagKey -> Shop.get().getSellShopItemTag(itemTagKey)).orElse(null);
            isShopItem = searchTag.isPresent();
        }
        if (!isShopItem) {
            AdminShop.LOGGER.debug("Item is not in shop sell map: "+toSell.getDisplayName().getString());
            return;
        }
        itemHandler.extractItem(0, count, false);
        long itemCost = shopItem.getPrice();
        long price = (long) count * itemCost;
        if (count == 0) {
            return;
        }
        // Get local MoneyManager and attempt transaction
        MoneyManager moneyManager = MoneyManager.get(level);

        // Check if account is set
        if (sellerEntity.account == null) {
            AdminShop.LOGGER.debug("Seller bankAccount is null");
            return;
        }
        // Check if account still exists
        if (!moneyManager.existsBankAccount(sellerEntity.account)) {
            AdminShop.LOGGER.debug("Seller machine account "+sellerEntity.account.getKey()+":"+sellerEntity.account
                    .getValue()+" does not exist");
            return;
        }
        String accOwner = sellerEntity.account.getKey();
        int accID = sellerEntity.account.getValue();
        // Check if account has necessary trade permit
        if (!moneyManager.getBankAccount(accOwner, accID).hasPermit(shopItem.getPermitTier())) {
            AdminShop.LOGGER.debug("Seller machine account does not have necessary trade permit");
            return;
        }
        boolean success = moneyManager.addBalance(accOwner, accID, price);
        if (!success) {
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

    private static boolean hasItem(SellerBE entity) {
        return !entity.itemHandler.getStackInSlot(0).isEmpty();
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
    }

    public void drops() {
        SimpleContainer inventory = new SimpleContainer(itemHandler.getSlots());
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            inventory.setItem(i, itemHandler.getStackInSlot(i));
        }

        Containers.dropContents(this.level, this.worldPosition, inventory);
    }
}
