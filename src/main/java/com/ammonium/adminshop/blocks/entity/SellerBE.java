package com.ammonium.adminshop.blocks.entity;

import com.ammonium.adminshop.AdminShop;
import com.ammonium.adminshop.blocks.ItemSellerMachine;
import com.ammonium.adminshop.money.BankAccount;
import com.ammonium.adminshop.money.MoneyManager;
import com.ammonium.adminshop.network.PacketSyncMoneyToClient;
import com.ammonium.adminshop.screen.SellerMenu;
import com.ammonium.adminshop.setup.Messages;
import com.ammonium.adminshop.shop.Shop;
import com.ammonium.adminshop.shop.ShopItem;
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
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class SellerBE extends BaseContainerBlockEntity implements ItemSellerMachine, WorldlyContainer {
    private String ownerUUID;
    private Pair<String, Integer> account;

    private int tickCounter = 0;
//    private final ItemStackHandler itemHandler = new ItemStackHandler(1) {
//        @Override
//        protected void onContentsChanged(int slot) {
//            setChanged();
//        }
//        @Override
//        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
//            // Check if item is in item map
//            boolean result = Shop.get().getShopSellItemMap().containsKey(stack.getItem());
//            if (!result) {
//                // Check if item tags are in item tags map
//                Optional<TagKey<Item>> searchTag = stack.getTags().filter(itemTag -> Shop.get().hasSellShopItemTag(itemTag)).findFirst();
//                result = searchTag.isPresent();
//            }
//            if (result) {
//                return super.isItemValid(slot, stack);
//            } else {
//                return false;
//            }
//        }
//    };

    private static final int slotSize = 1;
    private final NonNullList<ItemStack> stacks = NonNullList.withSize(slotSize, ItemStack.EMPTY);
    private final int[] slots = stacks.stream().mapToInt(stacks::indexOf).toArray();

    public SellerBE(BlockPos pWorldPosition, BlockState pBlockState) {
        super(ModBlockEntities.SELLER.get(), pWorldPosition, pBlockState);
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
        LazyOptional<IItemHandler> lazyHandler = sellerEntity.getCapability(ForgeCapabilities.ITEM_HANDLER);
        if (!lazyHandler.isPresent()) {
            AdminShop.LOGGER.debug("Seller item handler is not present");
            return;
        }
        IItemHandler handler = lazyHandler.orElseThrow(NullPointerException::new);
        ItemStack toSell = handler.getStackInSlot(0);
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
        handler.extractItem(0, count, false);
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
        return !entity.stacks.stream().allMatch(ItemStack::isEmpty);
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
    }

    public void drops() {
        Containers.dropContents(this.level, this.worldPosition, stacks);
    }

    @Override
    public int[] getSlotsForFace(Direction direction) {
        return slots;
    }

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
