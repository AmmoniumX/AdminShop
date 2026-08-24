package com.ammonium.adminshop.network;

import com.ammonium.adminshop.AdminShop;
import com.ammonium.adminshop.money.MoneyHelper;
import com.ammonium.adminshop.recipes.RecipeManager;
import com.ammonium.adminshop.recipes.SellFluidRecipe;
import com.ammonium.adminshop.recipes.SellItemRecipe;
import com.ammonium.adminshop.recipes.interfaces.SellRecipe;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.wrapper.PlayerMainInvWrapper;
import net.minecraftforge.network.NetworkEvent;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

public class PacketSellRequest {
    private int quantity;
    private final UUID teamId;
    private int slotIndex;
    private final ResourceLocation recipeId;

    public PacketSellRequest(UUID teamId, ResourceLocation recipeId, int slotIndex, int quantity){
        this.teamId = teamId;
        this.slotIndex = slotIndex;
        this.recipeId = recipeId;
        this.quantity = quantity;
    }

    public PacketSellRequest(FriendlyByteBuf buf){
        this.teamId = buf.readUUID();
        this.slotIndex = buf.readInt();
        this.recipeId = buf.readResourceLocation();
        this.quantity = buf.readInt();
    }

    public void toBytes(FriendlyByteBuf buf){
        buf.writeUUID(teamId);
        buf.writeInt(slotIndex);
        buf.writeResourceLocation(recipeId);
        buf.writeInt(quantity);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier){
        NetworkEvent.Context ctx = supplier.get();
        ctx.enqueueWork(() -> {
            //Client side accessed here
            //Do NOT call client-only code though, since server needs to access this too
//            AdminShop.LOGGER.debug("Performing sell transaction: {}, {}, {}", recipeId, slotIndex, quantity);
            ServerPlayer player = ctx.getSender();
            assert player != null;
            ServerLevel level = player.serverLevel();
            // Get item handler
            Inventory playerInventory = player.getInventory();
            IItemHandler itemHandler = LazyOptional.of(() -> new PlayerMainInvWrapper(playerInventory)).orElse(null);
            if (itemHandler == null) {
                AdminShop.LOGGER.debug("Item handler is null");
                return;
            }
            Optional<? extends Recipe<?>> recipeOptional = level.getRecipeManager().byKey(recipeId);
            if (recipeOptional.isEmpty() || !(recipeOptional.get() instanceof SellRecipe recipe)) {
                AdminShop.LOGGER.debug("Recipe is not a SellRecipe");
                return;
            }

            // Check if account has permit requirement
            if (!MoneyHelper.get(level).hasPermit(teamId, recipe.getPermit())) {
                AdminShop.LOGGER.error("Account {} does not have permit {}", teamId, recipe.getPermit());
                player.sendSystemMessage(Component.translatable("message.adminshop.no_permit", recipe.getPermit()));
                return;
            }

            if (recipe instanceof SellItemRecipe itemRecipe) {
                // Search for a valid sell stack
                ItemStack sellStack = ItemStack.EMPTY;
                // Check if we were given the item index
                if (slotIndex != -1) {
                    sellStack = itemHandler.getStackInSlot(slotIndex);

                    // Check if items match
                    if (!itemRecipe.isMatchingItemStack(sellStack)) {
                        AdminShop.LOGGER.debug("Item doesn't match recipe");
                        return;
                    }

                } else {
                    // Check if item is in inventory
                    int targetCount = (quantity > 0) ? itemRecipe.getCount() * quantity
                            : itemRecipe.getCount();
                    for (int i = 0; i < itemHandler.getSlots(); i++) {
                        ItemStack currentStack = itemHandler.getStackInSlot(i);
//                        AdminShop.LOGGER.debug("Checking stack {} against recipe: {}", currentStack, itemRecipe);
                        if (itemRecipe.isMatchingItemStack(currentStack) && currentStack.getCount() >= targetCount) {
                            AdminShop.LOGGER.debug("Found item in slot {}: {}", i, currentStack);
                            slotIndex = i;
                            sellStack = currentStack;
                            break;
                        }
                    }
                    if (sellStack.isEmpty()) {
                        AdminShop.LOGGER.debug("Could not find item");
                        return;
                    }
                }

                // Check if we found a valid item
                if (sellStack.isEmpty() || slotIndex == -1) {
                    AdminShop.LOGGER.debug("Could not find item in inventory");
                    return;
                }

                // Check if quantities match
                if (quantity > 0) {
                    if (sellStack.getCount() < itemRecipe.getCount() * quantity) {
                        AdminShop.LOGGER.debug("Not enough items to sell");
                        return;
                    }
                } else {
                    // If quantity is not provided, we want to sell as many items as we can in one "batch"
                    quantity = sellStack.getCount() / itemRecipe.getCount();
                }

                // Execute the sell
                AdminShop.LOGGER.debug("Selling item: {} x{}", sellStack.getDisplayName().getString(), itemRecipe.getCount() * quantity);
                sellItem(supplier, slotIndex, itemRecipe, quantity);

            } else if (recipe instanceof SellFluidRecipe fluidRecipe) {
                // Search for a valid sell stack
                ItemStack sellStack = ItemStack.EMPTY;
                // Check if we were given the item index
                if (slotIndex != -1) {
                    sellStack = itemHandler.getStackInSlot(slotIndex);

                    // Check if fluid container
                    boolean isFluidContainer = sellStack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).isPresent();
                    AdminShop.LOGGER.debug("Is fluid container: {}", isFluidContainer);
                    if (!isFluidContainer) {
                        return;
                    }

                    // Check if fluid matches
                    IFluidHandlerItem fluidHandler = sellStack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).orElse(null);
                    int targetAmount = (quantity > 0) ? fluidRecipe.getFluid().getAmount() * quantity : fluidRecipe.getFluid().getAmount();
                    boolean found = false;
                    for (int j = 0; j < fluidHandler.getTanks(); j++) {
                        FluidStack fluidStack = fluidHandler.getFluidInTank(j);
                        if (fluidStack.isEmpty()) { continue; }
                        // Check if the fluid matches
                        if (RecipeManager.matches(fluidStack, fluidRecipe.getFluid()) &&
                                fluidStack.getAmount() >= targetAmount) {
                            found = true;
                            break;
                        }
                    }
                    AdminShop.LOGGER.debug("Found matching fluid: {}", found);
                    if (!found) {
                        return;
                    }

                } else {
                    // Check if we have a valid fluid container with the fluid
                    int targetAmount = (quantity > 0) ? fluidRecipe.getFluid().getAmount() * quantity
                            : fluidRecipe.getFluid().getAmount();
                    for (int i = 0; i < itemHandler.getSlots(); i++) {
                        ItemStack currentStack = itemHandler.getStackInSlot(i);
                        LazyOptional<IFluidHandlerItem> lazyFluidHandler = currentStack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM);
                        if (!lazyFluidHandler.isPresent()) { continue; }
                        IFluidHandlerItem fluidHandler = currentStack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).orElse(null);
                        for (int j = 0; j < fluidHandler.getTanks(); j++) {
                            FluidStack fluidStack = fluidHandler.getFluidInTank(j);
                            if (fluidStack.isEmpty()) { continue; }
                            // Check if the fluid matches
                            if (RecipeManager.matches(fluidStack, fluidRecipe.getFluid()) &&
                                fluidStack.getAmount() >= targetAmount) {
                                AdminShop.LOGGER.debug("Found matching fluid in slot {}: {}", i, currentStack);
                                slotIndex = i;
                                sellStack = currentStack;
                                break;
                            }
                        }
                        if (!sellStack.isEmpty()) { break; }
                    }
                }

                // Check if we found a valid item
                if (sellStack.isEmpty() || slotIndex == -1) {
                    AdminShop.LOGGER.debug("Could not find fluid in inventory");
                    return;
                }

                int tankNumber = -1;
                IFluidHandlerItem fluidHandler = sellStack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).orElse(null);
                assert fluidHandler != null; // It should be false at this point
                if (quantity > 0) {
                    // Make sure quantities match
                    int targetAmount = fluidRecipe.getFluid().getAmount() * quantity;
                    for (int i = 0; i < fluidHandler.getTanks(); i++) {
                        FluidStack fluidStack = fluidHandler.getFluidInTank(i);
                        if (fluidStack.isEmpty()) { continue; }
                        // Check if the fluid matches
                        if (RecipeManager.matches(fluidStack, fluidRecipe.getFluid()) &&
                                fluidStack.getAmount() >= targetAmount) {
                            tankNumber = i;
                            break;
                        }
                    }
                } else {
                    // Find first tank with enough fluid
                    int targetAmount = fluidRecipe.getFluid().getAmount();
                    for (int i = 0; i < fluidHandler.getTanks(); i++) {
                        FluidStack fluidStack = fluidHandler.getFluidInTank(i);
                        if (fluidStack.isEmpty()) { continue; }
                        // Check if the fluid matches
                        if (RecipeManager.matches(fluidStack, fluidRecipe.getFluid()) &&
                                fluidStack.getAmount() >= targetAmount) {
                            tankNumber = i;
                            break;
                        }
                    }
                    quantity = 1;
                }

                if (tankNumber == -1) {
                    AdminShop.LOGGER.debug("Not enough fluid to sell");
                    return;
                }

                // Execute the sell
                AdminShop.LOGGER.debug("Selling fluid: {} x{}", sellStack.getDisplayName().getString(), fluidRecipe.getFluid().getAmount() * quantity);
                sellFluid(supplier, slotIndex, fluidRecipe, quantity);
            } else {
                AdminShop.LOGGER.debug("Recipe is not a SellItemRecipe");
                return;
            }
        });
        return true;
    }

    private void sellItem(Supplier<NetworkEvent.Context> supplier, int slotIndex, SellItemRecipe recipe, int sellQuantity) {
        // Assumes all checks have been done before calling this
        NetworkEvent.Context ctx = supplier.get();
        ServerPlayer player = ctx.getSender();
        assert player != null;
        ServerLevel level = player.serverLevel();
        Inventory playerInventory = player.getInventory();
        IItemHandler itemHandler = LazyOptional.of(() -> new PlayerMainInvWrapper(playerInventory)).orElse(null);
        int quantity = recipe.getCount() * sellQuantity;
        int numSold = itemHandler.extractItem(slotIndex, quantity, false).getCount();
        long price = sellQuantity * recipe.getPrice();
        if (numSold != quantity) {
            AdminShop.LOGGER.debug("Target quantity and extracted value don't match: {}, {}", quantity, numSold);
            return;
        }
        MoneyHelper.get(level).addMoney(teamId, price);
    }

    private void sellFluid(Supplier<NetworkEvent.Context> supplier, int slotIndex, SellFluidRecipe recipe, int sellQuantity) {
        // Assumes all checks have been done before calling this
        NetworkEvent.Context ctx = supplier.get();
        ServerPlayer player = ctx.getSender();
        assert player != null;
        ServerLevel level = player.serverLevel();
        Inventory playerInventory = player.getInventory();
        IItemHandler itemHandler = LazyOptional.of(() -> new PlayerMainInvWrapper(playerInventory)).orElse(null);
        ItemStack toExtract = itemHandler.getStackInSlot(slotIndex);
        IFluidHandlerItem fluidHandler = toExtract.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).orElse(null);
        int quantity = sellQuantity * recipe.getCount();
        FluidStack toDrain = recipe.getFluid().copy();
        toDrain.setAmount(quantity);
        FluidStack drained = fluidHandler.drain(toDrain, IFluidHandler.FluidAction.EXECUTE);
        long price = sellQuantity * recipe.getPrice();
        if (drained.getAmount() != quantity) {
            AdminShop.LOGGER.debug("Target quantity and extracted value don't match: {}, {}", quantity, drained.getAmount());
            return;
        }

        // Replace item
        ItemStack returned = fluidHandler.getContainer();
        if (!returned.equals(toExtract)) {
            itemHandler.extractItem(slotIndex, 1, false);
            ItemStack inserted = ItemHandlerHelper.insertItemStacked(itemHandler, returned, false);
            if (inserted.getCount() != 0) {
                AdminShop.LOGGER.debug("Error inserting fluid container: {}, inserted {}", inserted, inserted.getCount());
            }
        }

        MoneyHelper.get(level).addMoney(teamId, price);
    }

}
