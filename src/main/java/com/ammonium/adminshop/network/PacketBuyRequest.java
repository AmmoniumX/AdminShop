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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.wrapper.PlayerMainInvWrapper;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

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

    private void buyItemTransaction(Supplier<NetworkEvent.Context> supplier, BuyItemRecipe recipe, int buyQuantity) {
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
            int quantity = buyQuantity * recipe.getCount();
            ItemStack toInsert = recipe.getItem().get();
            toInsert.setCount(quantity);
            ItemStack returned = ItemHandlerHelper.insertItemStacked(iItemHandler, toInsert, true);
//            AdminShop.LOGGER.debug("Returned:{}x {}", returned.getCount(), returned);
            if(returned.getCount() == quantity) {
                player.sendSystemMessage(Component.literal("Not enough inventory space for item!"));
            }
            long price = buyQuantity * recipe.getPrice();

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
    private void buyFluidTransaction(Supplier<NetworkEvent.Context> supplier, BuyFluidRecipe recipe, int buyQuantity) {
        AdminShop.LOGGER.debug("buyFluidTransaction: {}, {}", recipe.getId(), buyQuantity);
        NetworkEvent.Context ctx = supplier.get();
        ServerPlayer player = ctx.getSender();
        assert player != null;
        ServerLevel level = player.getLevel();
        // Get item handler
        Inventory playerInventory = player.getInventory();
        LazyOptional<IItemHandler> playerInventoryHandlerCap = LazyOptional.of(() -> new PlayerMainInvWrapper(playerInventory));
        if (!playerInventoryHandlerCap.isPresent()) {
            AdminShop.LOGGER.error("No item handler found for player inventory");
            return;
        }
        IItemHandler playerInventoryHandler = playerInventoryHandlerCap.orElse(null);
        // fluid logic
        // Attempt to insert the fluid into a IFluidContainerItem, and only perform transaction on what can fit (up to 1000mb)
        AdminShop.LOGGER.debug("Buying Fluid");
        FluidStack toInsert = recipe.getFluid().copy();
        int quantity = buyQuantity * recipe.getCount();
        toInsert.setAmount(quantity);
        int fillableContainerIdx = getFillableFluidContainer(playerInventoryHandler, toInsert.getFluid(), quantity);
        if(fillableContainerIdx == -1) {
            player.sendSystemMessage(Component.literal("No container found for fluid!"));
            AdminShop.LOGGER.error("No container found for fluid.");
            return;
        }
        ItemStack container = playerInventoryHandler.getStackInSlot(fillableContainerIdx);
        // If stacked buckets, make sure you have an empty slot
        if (container.getItem().equals(Items.BUCKET) && container.getCount() != 1) {
            if (!hasEmptySlot(playerInventoryHandler)) {
                player.sendSystemMessage(Component.literal("Trying to fill into a bucket, but wouldn't have space for filled bucket"));
                AdminShop.LOGGER.debug("Trying to fill into a bucket, but wouldn't have space for filled bucket");
                return;
            }
            // Set new container to be single item
            ItemStack singleStack = container.copy();
            singleStack.setCount(1);
            container = singleStack;
        }
        LazyOptional<IFluidHandlerItem> containerHandlerCap = container.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM);
        if (!containerHandlerCap.isPresent()) {
            AdminShop.LOGGER.error("No fluid handler found for container");
            return;
        }
        IFluidHandlerItem containerHandler = containerHandlerCap.orElse(null);
        int filledQuantity = containerHandler.fill(toInsert, IFluidHandler.FluidAction.SIMULATE);
        if (filledQuantity != quantity) {
            AdminShop.LOGGER.error("Not enough space in container for fluid: {} / {}", filledQuantity, quantity);
            return;
        }
        toInsert.setAmount(filledQuantity);
        long price = recipe.getPrice() * buyQuantity;

//        MoneyHelper.MoneyAccount account = MoneyHelper.get(level).getAccountById(teamId);
        boolean success = MoneyHelper.get(level).removeMoney(teamId, price);
        if (success) {
            AdminShop.LOGGER.debug("Attempt to fill with {}, {}", toInsert.getDisplayName().getString(), toInsert.getAmount());
            int filled = containerHandler.fill(toInsert, IFluidHandler.FluidAction.EXECUTE);
            AdminShop.LOGGER.debug("Filled with {} mb", filled);

            // Replace item
            ItemStack newContainer = containerHandler.getContainer();
            AdminShop.LOGGER.debug("New container: "+newContainer);
            if (!newContainer.equals(container)) {
                AdminShop.LOGGER.debug("Removing old container");
                playerInventoryHandler.extractItem(fillableContainerIdx, 1, false);
                AdminShop.LOGGER.debug("Giving new container");
                ItemStack inserted = ItemHandlerHelper.insertItemStacked(playerInventoryHandler, newContainer, false);
                AdminShop.LOGGER.debug("Inserted: "+inserted);
                if (inserted.getCount() != 0) {
                    player.sendSystemMessage(Component.literal("Error inserting fluid container, this shouldn't happen!"));
                    AdminShop.LOGGER.error("Error inserting fluid container, this shouldn't happen! {}", inserted.getCount());
                }
            }
        } else {
            player.sendSystemMessage(Component.literal("Not enough money in account!"));
            AdminShop.LOGGER.debug("Not enough money in account to perform transaction.");
        }
    }

    /**
     * Finds first possible container that can get filled with flujd
     * @param itemHandler handler for player inventory
     * @param fluid type of fluid
     * @param quantity amount of fluid to fill
     * @return the index of the first container that can be filled with the fluid, or -1 if none found
     */
    public static int getFillableFluidContainer(IItemHandler itemHandler, Fluid fluid, int quantity) {
        // Iterate over all slots in the IItemHandler
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            // Get the ItemStack in the current slot
            ItemStack stack = itemHandler.getStackInSlot(i);
            // return if stack is empty
            if (stack.isEmpty()) {continue;}
            // If bucket, only return if quantity is exactly 1b
            if (stack.getItem().equals(Items.BUCKET) && quantity == 1000) {
                // Also, check if the stack has more than 1 bucket, if so, we need an empty slot
                if (stack.getCount() > 1 && !hasEmptySlot(itemHandler)) {
                    AdminShop.LOGGER.debug("Trying to fill into stacked buckets, but wouldn't have space for filled " +
                            "bucket. Skipping");
                    continue;
                }

                // Check if we know the fluid bucket
                Item bucketItem = fluid.getBucket();
                if (bucketItem != null && bucketItem != Items.AIR) { return i; }
            }
            // Check if the ItemStack has the IFluidHandlerItem capability
            LazyOptional<IFluidHandlerItem> fluidHandlerCapability = stack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM);
            if (!fluidHandlerCapability.isPresent()) {
                AdminShop.LOGGER.debug("ItemStack {} does not have fluid handler capability", stack.getDisplayName().getString());
                continue;
            }
            AdminShop.LOGGER.debug("Found fluid container on slot "+i+": "+ stack.getDisplayName().getString());
            if (!fluidHandlerCapability.isPresent()) {
                AdminShop.LOGGER.debug("ItemStack {} does not have fluid handler capability", stack.getDisplayName().getString());
                continue;
            }
            // Check if we can fill the fluid into the container
            IFluidHandlerItem newFluidHandler = fluidHandlerCapability.orElse(null);
            AdminShop.LOGGER.debug("Checking if container can accept fluid: {}, {}", stack.getDisplayName().getString(), quantity);
            int canFillAmount = newFluidHandler.fill(new FluidStack(fluid, quantity), IFluidHandler.FluidAction.SIMULATE);
            AdminShop.LOGGER.debug("Can fill amount: {}", canFillAmount);
            if (canFillAmount > 0) {
                // If we can fill, set the result
                AdminShop.LOGGER.debug("Container can accept fluid: {}, {}, {}", stack.getDisplayName().getString(), fluid.getFluidType(),canFillAmount);
                return i;
            }
        }
        // If no suitable fluid containers were found, return -1
        return -1;
    }
    public static boolean hasEmptySlot(IItemHandler itemHandler) {
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            ItemStack stack = itemHandler.getStackInSlot(i);
            if (stack.isEmpty()) {
                return true;
            }
        }
        return false;
    }

}
