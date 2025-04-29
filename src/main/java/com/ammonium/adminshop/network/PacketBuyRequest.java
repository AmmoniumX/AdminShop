package com.ammonium.adminshop.network;

import com.ammonium.adminshop.AdminShop;
import com.ammonium.adminshop.money.MoneyHelper;
import com.ammonium.adminshop.recipes.BuyFluidRecipe;
import com.ammonium.adminshop.recipes.BuyItemRecipe;
import com.ammonium.adminshop.recipes.interfaces.BuyRecipe;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.wrapper.PlayerMainInvWrapper;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import static java.lang.Math.ceil;

public class PacketBuyRequest {
    private final int quantity;
    private final UUID teamId;
    private final ResourceLocation recipeId; // final

    public PacketBuyRequest(UUID teamId, ResourceLocation recipeId, int quantity){
        this.teamId = teamId;
        this.recipeId = recipeId;
        this.quantity = quantity;
    }

    public PacketBuyRequest(FriendlyByteBuf buf){
        this.teamId = buf.readUUID();
        this.recipeId = buf.readResourceLocation();
        this.quantity = buf.readInt();
    }

    public void toBytes(FriendlyByteBuf buf){
        buf.writeUUID(teamId);
        buf.writeResourceLocation(recipeId);
        buf.writeInt(quantity);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier){
        NetworkEvent.Context ctx = supplier.get();
        ctx.enqueueWork(() -> {
            //Client side accessed here
            //Do NOT call client-only code though, since server needs to access this too
            ServerPlayer player = ctx.getSender();
            assert player != null;
            ServerLevel level = ctx.getSender().getLevel();

            Recipe<?> rawRecipe = level.getRecipeManager().byKey(recipeId).orElse(null);
            if (!(rawRecipe instanceof BuyRecipe recipe)) {
                AdminShop.LOGGER.error("Not a valid BuyRecipe: {}", recipeId);
                return;
            }

            AdminShop.LOGGER.debug("Performing buy transaction: ");
            if (recipe instanceof BuyItemRecipe itemRecipe) {
                AdminShop.LOGGER.debug("Item: {}", itemRecipe.getItem());
            } else if (recipe instanceof BuyFluidRecipe fluidRecipe){
                AdminShop.LOGGER.debug("Fluid: {}", fluidRecipe.getFluid());
            }

            // Check if account has permit requirement
            if (!MoneyHelper.get(level).hasPermit(teamId, recipe.getPermit())) {
                AdminShop.LOGGER.error("Account {} does not have permit {}", teamId, recipe.getPermit());
                player.sendSystemMessage(Component.literal( "Your account does not have permit '"+recipe.getPermit()+"'"));
                return;
            }

            if (recipe instanceof BuyItemRecipe itemRecipe) {
                buyItemTransaction(supplier, itemRecipe, quantity);
            } else if (recipe instanceof BuyFluidRecipe fluidRecipe) {
                buyFluidTransaction(supplier, fluidRecipe, quantity);
            } else {
                AdminShop.LOGGER.error("Not a valid BuyRecipe: {}", recipeId);
                return;
            }

//            // Sync money with affected clients
//            AdminShop.LOGGER.debug("Syncing money with clients");
//            // Get current bank account
//            BankAccount currentAccount = moneyManager.getBankAccount(this.accOwner, this.accID);
//
//            // Sync money with bank account's members
//            assert currentAccount.getMembers().contains(this.accOwner);
//            currentAccount.getMembers().forEach(memberUUID -> {
//                List<BankAccount> usableAccounts = moneyManager.getSharedAccounts().get(memberUUID);
//                ServerPlayer serverPlayer = (ServerPlayer) player.getLevel()
//                        .getPlayerByUUID(UUID.fromString(memberUUID));
//                if (serverPlayer == null) return;
//                Messages.sendToPlayer(new PacketSyncMoneyToClient(usableAccounts), serverPlayer);
//            });
        });
        return true;
    }

    private void buyItemTransaction(Supplier<NetworkEvent.Context> supplier, BuyItemRecipe recipe, int quantity) {
        // IItemHandler inventory = player.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null);
        NetworkEvent.Context ctx = supplier.get();
        ServerPlayer player = ctx.getSender();
        assert player != null;
        ServerLevel level = player.getLevel();
        // Get item handler
        Inventory playerInventory = player.getInventory();
        LazyOptional<IItemHandler> mainInventoryHandler = LazyOptional.of(() -> new PlayerMainInvWrapper(playerInventory));
        mainInventoryHandler.ifPresent(iItemHandler -> {
            // item logic
            // Attempt to insert the items, and only perform transaction on what can fit
            AdminShop.LOGGER.debug("Buying Item");
            int numItems = quantity * recipe.getCount();
            ItemStack toInsert = recipe.getItem().get();
            toInsert.setCount(numItems);
            ItemStack returned = ItemHandlerHelper.insertItemStacked(iItemHandler, toInsert, true);
//            AdminShop.LOGGER.debug("Returned:{}x {}", returned.getCount(), returned);
            if(returned.getCount() == numItems) {
                player.sendSystemMessage(Component.literal("Not enough inventory space for item!"));
            }
            long price = quantity * recipe.getPrice();

            MoneyHelper.MoneyAccount account = MoneyHelper.get(level).getAccountById(teamId);
            boolean success = MoneyHelper.get(level).removeMoney(teamId, price);
            if (success) {
                ItemHandlerHelper.insertItemStacked(iItemHandler, toInsert, false);
            } else {
                player.sendSystemMessage(Component.literal("Not enough money in account!"));
                AdminShop.LOGGER.debug("Not enough money in account to perform transaction.");
            }
        });
    }
    private void buyFluidTransaction(Supplier<NetworkEvent.Context> supplier, BuyFluidRecipe recipe, int quantity) {
        // TODO fix not working with buckets
        NetworkEvent.Context ctx = supplier.get();
        ServerPlayer player = ctx.getSender();
        assert player != null;
        ServerLevel level = player.getLevel();
        // Get item handler
        Inventory playerInventory = player.getInventory();
        LazyOptional<IItemHandler> mainInventoryHandler = LazyOptional.of(() -> new PlayerMainInvWrapper(playerInventory));
        mainInventoryHandler.ifPresent(itemHandler -> {
            // fluid logic
            // Attempt to insert the fluid into a IFluidContainerItem, and only perform transaction on what can fit (up to 1000mb)
            AdminShop.LOGGER.debug("Buying Fluid");
            FluidStack toInsert = recipe.getFluid().copy();
            toInsert.setAmount(quantity);
            int fillableContainerIdx = getFillableFluidContainer(itemHandler, toInsert.getFluid(), quantity);
            if(fillableContainerIdx == -1) {
                player.sendSystemMessage(Component.literal("No container found for fluid!"));
                AdminShop.LOGGER.error("No container found for fluid.");
                return;
            }
            ItemStack ogContainer = itemHandler.getStackInSlot(fillableContainerIdx);
            AtomicReference<ItemStack> newContainer = new AtomicReference<>(ogContainer);
            // If stacked buckets, make sure you have an empty slot
            if (ogContainer.getItem().equals(Items.BUCKET) && ogContainer.getCount() != 1) {
                if (!hasEmptySlot(itemHandler)) {
                    player.sendSystemMessage(Component.literal("Trying to fill into a bucket, but wouldn't have space for filled bucket"));
                    AdminShop.LOGGER.debug("Trying to fill into a bucket, but wouldn't have space for filled bucket");
                    return;
                }
                // Set new container to be single item
                ItemStack singleStack = ogContainer.copy();
                singleStack.setCount(1);
                newContainer.set(singleStack);
            }
            AtomicInteger filledAmount = new AtomicInteger();
            newContainer.get().getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).ifPresent(handler ->
                    filledAmount.set(handler.fill(toInsert, IFluidHandler.FluidAction.SIMULATE)));
            toInsert.setAmount(filledAmount.get());
            long fluidCost = recipe.getPrice();
            long price = (long) ceil(filledAmount.get() * fluidCost);

            MoneyHelper.MoneyAccount account = MoneyHelper.get(level).getAccountById(teamId);
            boolean success = MoneyHelper.get(level).removeMoney(teamId, price);
            if (success) {
                newContainer.get().getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).ifPresent(fluidHandler -> {
                    AdminShop.LOGGER.debug("Attempt to fill with {}, {}", toInsert.getDisplayName().getString(), toInsert.getAmount());
                    int filled = fluidHandler.fill(toInsert, IFluidHandler.FluidAction.EXECUTE);
                    AdminShop.LOGGER.debug("Filled with {} mb", filled);

                    // Replace item
                    ItemStack newBucket = fluidHandler.getContainer();
//                    AdminShop.LOGGER.debug("New container: "+newBucket);
                    if (!newBucket.equals(ogContainer)) {
//                        AdminShop.LOGGER.debug("Giving new container");
                        itemHandler.extractItem(fillableContainerIdx, 1, false);
                        ItemStack inserted = ItemHandlerHelper.insertItemStacked(itemHandler, newBucket, false);
//                        AdminShop.LOGGER.debug("Inserted: "+inserted);
                        if (inserted.getCount() != 0) {
                            player.sendSystemMessage(Component.literal("Error inserting fluid container, this shouldn't happen!"));
                            AdminShop.LOGGER.error("Error inserting fluid container, this shouldn't happen! {}", inserted.getCount());
                        }
                    }
                    });
            } else {
                player.sendSystemMessage(Component.literal("Not enough money in account!"));
                AdminShop.LOGGER.debug("Not enough money in account to perform transaction.");
            }
        });
    }

    /**
     * Finds first possible container that can get filled with flujd
     * @param itemHandler handler for player inventory
     * @param fluid type of fluid
     * @return pair where first value is the slot index and second value is max fillable fluid
     */
    public static int getFillableFluidContainer(IItemHandler itemHandler, Fluid fluid, int quantity) {
        // Iterate over all slots in the IItemHandler
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            // Get the ItemStack in the current slot
            ItemStack ogStack = itemHandler.getStackInSlot(i);
            // return if stack is empty
            if (ogStack.isEmpty()) {continue;}
            AtomicReference<ItemStack> stack = new AtomicReference<>(ogStack);
            AtomicInteger result = new AtomicInteger(-1);
            // Check if the ItemStack has the IFluidHandlerItem capability
            int finalI = i;
            ogStack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).ifPresent(ogHandler -> {
                AdminShop.LOGGER.debug("Found fluid container on slot "+finalI+": "+ stack.get().getDisplayName().getString());
                // If stacked buckets, check if you can insert into single bucket
                if (ogStack.getItem().equals(Items.BUCKET) && ogStack.getCount() > 1) {
                    ItemStack singleItem = ogStack.copy();
                    singleItem.setCount(1);
                    stack.set(singleItem);
                }
                stack.get().getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).ifPresent(newHandler -> {
                    // Check if we can fill the fluid into the container
                    int canFillAmount = newHandler.fill(new FluidStack(fluid, quantity), IFluidHandler.FluidAction.SIMULATE);
                    if (canFillAmount > 0) {
                        // If we can fill, set the result
                        AdminShop.LOGGER.debug("Container can fill: " + canFillAmount);
                        result.set(finalI);
                    }
                });
            });

            // If result is set, return it
            if (result.get() != -1) {
                return result.get();
            }
        }
        // If no suitable fluid containers were found, return -1
        return -1;
    }
    public boolean hasEmptySlot(IItemHandler itemHandler) {
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            ItemStack stack = itemHandler.getStackInSlot(i);
            if (stack.isEmpty()) {
                return true;
            }
        }
        return false;
    }

}
