package com.ammonium.adminshop.blocks.entity;

import com.ammonium.adminshop.AdminShop;
import com.ammonium.adminshop.blocks.FluidSellerMachine;
import com.ammonium.adminshop.money.BankAccount;
import com.ammonium.adminshop.money.MoneyManager;
import com.ammonium.adminshop.network.PacketSyncMoneyToClient;
import com.ammonium.adminshop.screen.FluidSellerMenu;
import com.ammonium.adminshop.setup.Messages;
import com.ammonium.adminshop.shop.Shop;
import com.ammonium.adminshop.shop.ShopItem;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.FluidHandlerBlockEntity;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.tags.IReverseTag;
import net.minecraftforge.registries.tags.ITagManager;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class FluidSellerBE extends FluidHandlerBlockEntity implements FluidSellerMachine {
    private String ownerUUID;
    private Pair<String, Integer> account;
    private int tickCounter = 0;
    private static final int TANK_CAPACITY = 64000;

    public FluidSellerBE(BlockPos pWorldPosition, BlockState pBlockState) {
        super(ModBlockEntities.FLUID_SELLER.get(), pWorldPosition, pBlockState);
//        this.tank = new InsertSellableOnlyTank(TANK_CAPACITY, this::sendUpdates);
        this.tank = new FluidTank(TANK_CAPACITY);
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
        return Component.translatable("block.adminshop.fluid_seller");
    }

    @Override
    public void setChanged() {
        super.setChanged();
    }

    public FluidTank getTank() {
        return this.tank;
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int pContainerId, Inventory pInventory, Player pPlayer) {
        return new FluidSellerMenu(pContainerId, pInventory, this);
    }

    public static void tick(Level pLevel, BlockPos pPos, BlockState pState, FluidSellerBE pBlockEntity) {
        if(!pLevel.isClientSide) {
            pBlockEntity.tickCounter++;
            if (pBlockEntity.tickCounter > 20) {
                pBlockEntity.tickCounter = 0;
                // Send sell fluid transaction
                assert pLevel instanceof ServerLevel;
                sellerTransaction(pPos, (ServerLevel) pLevel, pBlockEntity);
            }
        }
    }

    public static void sellerTransaction(BlockPos pos, ServerLevel level, FluidSellerBE sellerEntity) {
        LazyOptional<IFluidHandler> lazyFluidHandler = sellerEntity.getCapability(ForgeCapabilities.FLUID_HANDLER);
        if (!lazyFluidHandler.isPresent()) {
            AdminShop.LOGGER.debug("FluidSeller has no FluidHandler!");
            return;
        }
        lazyFluidHandler.ifPresent(fluidHandler -> {
//            AdminShop.LOGGER.debug("fluid handler logic");
            // fluid logic
            // Attempt to insert the fluids, and only perform transaction on what can fit
            MoneyManager moneyManager = MoneyManager.get(level);
            // Check if Shop has fluid or fluid tag
            FluidStack fluidStack = fluidHandler.getFluidInTank(0).copy();
            // Return if empty
            if (fluidStack.isEmpty()) {
//                AdminShop.LOGGER.debug("FluidSeller tank is empty!");
                return;
            }
            ShopItem shopItem = null;
            Fluid fluid = fluidStack.getFluid();
//            AdminShop.LOGGER.debug("FluidStack: "+fluidStack.getAmount()+"mb "+fluidStack.getDisplayName().getString());
            // First check if in sell fluid map
            boolean isValidFluid = Shop.get().hasSellShopFluid(fluid);
            // Then check if fluid tags in fluid tag map
            if (isValidFluid) {
                shopItem = Shop.get().getSellShopFluid(fluid);
            } else {
                ITagManager<Fluid> fluidTagManager = ForgeRegistries.FLUIDS.tags();
                if (fluidTagManager == null) {
                    AdminShop.LOGGER.debug("fluidTagManager is null!");
                    return;
                }
                Optional<IReverseTag<Fluid>> oFluidReverseTag = fluidTagManager.getReverseTag(fluid);
                if (oFluidReverseTag.isPresent()) {
                    IReverseTag<Fluid> fluidReverseTag = oFluidReverseTag.get();
                    Optional<TagKey<Fluid>> oFluidTag = fluidReverseTag.getTagKeys().filter(fluidTag -> Shop.get().hasSellShopFluidTag(fluidTag)).findFirst();
                    if (oFluidTag.isPresent()) {
                        TagKey<Fluid> tag = oFluidTag.get();
                        shopItem = Shop.get().getSellShopFluidTag(tag);
                    }
                }
            }
            // Check if shopItem is null
            if (shopItem == null) {
                AdminShop.LOGGER.debug("shopItem is null!");
                return;
            }
            // Check shopItem is fluid and sell only
            if (shopItem.isBuy() || shopItem.isItem()) {
                AdminShop.LOGGER.debug("Fluid Seller shopItem is not sell fluid!");
                return;
            }
            if (shopItem.getFluid().isEmpty()) {
                AdminShop.LOGGER.debug("Fluid Seller shopItem is empty!");
                return;
            }
//            AdminShop.LOGGER.debug("Found valid fluid: "+shopItem.getFluid().getDisplayName().getString());
            FluidStack toDrain = fluidHandler.drain(fluidStack, IFluidHandler.FluidAction.SIMULATE);
            if(toDrain.isEmpty()) {
//                AdminShop.LOGGER.debug("toDrain is empty!");
                return;
            }
            long itemCost = shopItem.getPrice();
            long price = (long) toDrain.getAmount() * itemCost;
            // Get MoneyManager and attempt transaction
            // Check if account is set
            if (sellerEntity.account == null) {
                AdminShop.LOGGER.debug("Fluid Seller bankAccount is null");
                return;
            }
            // Check if account still exists
            if (!moneyManager.existsBankAccount(sellerEntity.account)) {
                AdminShop.LOGGER.debug("Fluid Seller machine account "+sellerEntity.account.getKey()+":"+sellerEntity.account
                        .getValue()+" does not exist");
                return;
            }
            String accOwner = sellerEntity.account.getKey();
            int accID = sellerEntity.account.getValue();

//            AdminShop.LOGGER.debug("Adding "+price+" to account for "+fluidStack.getAmount()+"mb");
            boolean success = true;
            // Only give money if has the permit to sell
            if (moneyManager.getBankAccount(accOwner, accID).hasPermit(shopItem.getPermitTier())) {
                success = moneyManager.addBalance(accOwner, accID, price);
            }
            if (success) {
                FluidStack drained = fluidHandler.drain(toDrain, IFluidHandler.FluidAction.EXECUTE);
//                AdminShop.LOGGER.debug("Successfully drained "+drained+"mb");
            } else {
                AdminShop.LOGGER.debug("Error selling fluid.");
                return;
            }
            // Sync account data
            // Get current bank account
            BankAccount currentAccount = moneyManager.getBankAccount(accOwner, accID);
            // Sync money with bank account's members
            assert currentAccount.getMembers().contains(accOwner);
            currentAccount.getMembers().forEach(memberUUID -> {
                List<BankAccount> usableAccounts = moneyManager.getSharedAccounts().get(memberUUID);
                ServerPlayer playerByUUID = (ServerPlayer) level.getPlayerByUUID(UUID.fromString(memberUUID));
                if (playerByUUID != null) {
                    Messages.sendToPlayer(new PacketSyncMoneyToClient(usableAccounts), playerByUUID);
                }
            });
        });
    }


//    @Nonnull
//    @Override
//    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @javax.annotation.Nullable Direction side) {
//        if (cap == ForgeCapabilities.FLUID_HANDLER) {
//            return lazyItemHandler.cast();
//        }
//        return super.getCapability(cap, side);
//    }

    @Override
    public void onLoad() {
        super.onLoad();
//        lazyItemHandler = LazyOptional.of(() -> itemHandler);
    }

    @Override
    public void invalidateCaps()  {
        super.invalidateCaps();
//        lazyItemHandler.invalidate();
    }

    @Override
    public @NotNull CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
//        tag.put("inventory", this.itemHandler.serializeNBT());
        tank.writeToNBT(tag);
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
//        this.itemHandler.deserializeNBT(tag.getCompound("inventory"));
        this.tank.readFromNBT(tag);
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
//        tank.writeToNBT(tag);
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
//        tank.readFromNBT(tag);
        if (tag.contains("ownerUUID")) {
            this.ownerUUID = tag.getString("ownerUUID");
        }
        if (tag.contains("accountUUID") && tag.contains("accountID")) {
            String accountUUID = tag.getString("accountUUID");
            int accountID = tag.getInt("accountID");
            this.account = Pair.of(accountUUID, accountID);
        }
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
