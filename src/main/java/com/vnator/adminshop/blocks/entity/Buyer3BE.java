package com.vnator.adminshop.blocks.entity;

import com.vnator.adminshop.AdminShop;
import com.vnator.adminshop.blocks.AutoShopMachine;
import com.vnator.adminshop.money.*;
import com.vnator.adminshop.network.PacketSyncMoneyToClient;
import com.vnator.adminshop.screen.Buyer3Menu;
import com.vnator.adminshop.setup.Messages;
import com.vnator.adminshop.shop.ShopItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
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
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.UUID;

import static java.lang.Math.ceil;

public class Buyer3BE extends BlockEntity implements AutoShopMachine {
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

    @Override
    public Component getDisplayName() {
        return new TextComponent("Auto-Buyer");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int pContainerId, Inventory pInventory, Player pPlayer) {
        return new Buyer3Menu(pContainerId, pInventory, this);
    }

    public static void tick(Level pLevel, BlockPos pPos, BlockState pState, Buyer3BE pBlockEntity) {
        if(ClientLocalData.hasTarget(pPos)) {
            pBlockEntity.tickCounter++;
            if (pBlockEntity.tickCounter > 20) {
                pBlockEntity.tickCounter = 0;
                // Send buy item transaction (send pos and buySize)
                if (!pLevel.isClientSide) {
                    assert pLevel instanceof ServerLevel;
                    buyerTransaction(pPos, (ServerLevel) pLevel, pBlockEntity, pBlockEntity.buySize);
                }
            }
        }
    }

    public static void buyerTransaction(BlockPos pos, ServerLevel level, Buyer3BE buyerEntity, int buySize) {
        System.out.println("Processing buyer transaction for "+pos+", "+buySize);
        // item logic
        // Attempt to insert the items, and only perform transaction on what can fit
        MoneyManager moneyManager = MoneyManager.get(level);
        MachineOwnerInfo machineOwnerInfo = MachineOwnerInfo.get(level);
        BuyerTargetInfo buyerTargetInfo = BuyerTargetInfo.get(level);
        ShopItem shopItem = buyerTargetInfo.getBuyerTarget(pos);
        Item item = shopItem.getItem().getItem();
        ItemStack toInsert = new ItemStack(item);
        toInsert.setCount(buySize);
        ItemStackHandler handler = buyerEntity.getItemHandler();
        ItemStack returned = ItemHandlerHelper.insertItemStacked(handler, toInsert, true);
        if(returned.getCount() == buySize) {
            return;
        }
        int itemCost = shopItem.getPrice();
        long price = (long) ceil((buySize - returned.getCount()) * itemCost);
        // Get MoneyManager and attempt transaction
        Pair<String, Integer> account = machineOwnerInfo.getMachineAccount(pos);
        String accOwner = account.getKey();
        int accID = account.getValue();
        // Check if account has enough money, if not reduce amount
        long balance = moneyManager.getBalance(accOwner, accID);
        if (price > balance) {
            if (itemCost > balance) {
                // not enough money to buy one
                return;
            }
            // Find max amount he can buy
            buySize = (int) (balance / itemCost);
            price = (long) ceil(buySize * itemCost);
            toInsert.setCount(buySize);
        }
        boolean success = moneyManager.subtractBalance(accOwner, accID, price);
        if (success) {
            ItemHandlerHelper.insertItemStacked(handler, toInsert, false);
            System.out.println("Bought item");
        } else {
            AdminShop.LOGGER.error("Error selling item.");
            return;
        }
        // Sync account data
        // Get current bank account
        BankAccount currentAccount = moneyManager.getBankAccount(accOwner, accID);
        // Sync money with bank account's members
        assert currentAccount.getMembers().contains(accOwner);
        currentAccount.getMembers().forEach(memberUUID -> {
            List<BankAccount> usableAccounts = moneyManager.getSharedAccounts().get(memberUUID);
            Messages.sendToPlayer(new PacketSyncMoneyToClient(usableAccounts), (ServerPlayer) level.
                    getPlayerByUUID(UUID.fromString(memberUUID)));
        });
    }

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @javax.annotation.Nullable Direction side) {
        if (cap == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
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
    protected void saveAdditional(@NotNull CompoundTag tag) {
        tag.put("inventory", this.itemHandler.serializeNBT());
        super.saveAdditional(tag);
    }

    @Override
    public void load(CompoundTag tag) {
        this.itemHandler.deserializeNBT(tag.getCompound("inventory"));
        super.load(tag);
    }

    public void drops() {
        SimpleContainer inventory = new SimpleContainer(itemHandler.getSlots());
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            inventory.setItem(i, itemHandler.getStackInSlot(i));
        }

        Containers.dropContents(this.level, this.worldPosition, inventory);
    }
}
