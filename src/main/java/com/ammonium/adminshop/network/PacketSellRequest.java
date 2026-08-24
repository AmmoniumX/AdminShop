package com.ammonium.adminshop.network;

import com.ammonium.adminshop.AdminShop;
import com.ammonium.adminshop.money.MoneyHelper;
import com.ammonium.adminshop.recipes.RecipeManager;
import com.ammonium.adminshop.recipes.SellFluidRecipe;
import com.ammonium.adminshop.recipes.SellItemRecipe;
import com.ammonium.adminshop.recipes.interfaces.SellRecipe;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.wrapper.PlayerMainInvWrapper;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.UUID;

public class PacketSellRequest implements CustomPacketPayload {

    public static final Type<PacketSellRequest> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(AdminShop.MODID, "sell_request"));
    public static final StreamCodec<FriendlyByteBuf, PacketSellRequest> STREAM_CODEC = StreamCodec.of(
            (buf, pkt) -> {
                buf.writeUUID(pkt.teamId);
                buf.writeInt(pkt.slotIndex);
                buf.writeResourceLocation(pkt.recipeId);
                buf.writeInt(pkt.quantity);
            },
            buf -> {
                UUID teamId = buf.readUUID();
                int slotIndex = buf.readInt();
                ResourceLocation recipeId = buf.readResourceLocation();
                int quantity = buf.readInt();
                return new PacketSellRequest(teamId, recipeId, slotIndex, quantity);
            }
    );

    private int quantity;
    private final UUID teamId;
    private int slotIndex;
    private final ResourceLocation recipeId;

    public PacketSellRequest(UUID teamId, ResourceLocation recipeId, int slotIndex, int quantity) {
        this.teamId = teamId;
        this.slotIndex = slotIndex;
        this.recipeId = recipeId;
        this.quantity = quantity;
    }

    public static void handle(PacketSellRequest packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            ServerLevel level = player.serverLevel();
            Inventory playerInventory = player.getInventory();
            IItemHandler itemHandler = new PlayerMainInvWrapper(playerInventory);

            RecipeHolder<?> rawHolder = level.getRecipeManager().byKey(packet.recipeId).orElse(null);
            if (rawHolder == null || !(rawHolder.value() instanceof SellRecipe recipe)) {
                AdminShop.LOGGER.debug("Recipe is not a SellRecipe");
                return;
            }

            if (!MoneyHelper.get(level).hasPermit(packet.teamId, recipe.getPermit())) {
                AdminShop.LOGGER.debug("Account {} does not have permit {}", packet.teamId, recipe.getPermit());
                player.sendSystemMessage(Component.translatable("message.adminshop.no_permit", recipe.getPermit()));
                return;
            }

            if (recipe instanceof SellItemRecipe itemRecipe) {
                ItemStack sellStack = ItemStack.EMPTY;
                if (packet.slotIndex != -1) {
                    sellStack = itemHandler.getStackInSlot(packet.slotIndex);
                    if (!itemRecipe.isMatchingItemStack(sellStack)) {
                        AdminShop.LOGGER.debug("Item doesn't match recipe");
                        return;
                    }
                } else {
                    int targetCount = (packet.quantity > 0) ? itemRecipe.getCount() * packet.quantity : itemRecipe.getCount();
                    for (int i = 0; i < itemHandler.getSlots(); i++) {
                        ItemStack currentStack = itemHandler.getStackInSlot(i);
                        if (itemRecipe.isMatchingItemStack(currentStack) && currentStack.getCount() >= targetCount) {
                            packet.slotIndex = i;
                            sellStack = currentStack;
                            break;
                        }
                    }
                    if (sellStack.isEmpty()) {
                        AdminShop.LOGGER.debug("Could not find item");
                        return;
                    }
                }

                if (sellStack.isEmpty() || packet.slotIndex == -1) {
                    AdminShop.LOGGER.debug("Could not find item in inventory");
                    return;
                }

                if (packet.quantity > 0) {
                    if (sellStack.getCount() < itemRecipe.getCount() * packet.quantity) {
                        AdminShop.LOGGER.debug("Not enough items to sell");
                        return;
                    }
                } else {
                    packet.quantity = sellStack.getCount() / itemRecipe.getCount();
                }

                packet.sellItem(player, level, itemHandler, packet.slotIndex, itemRecipe, packet.quantity);

            } else if (recipe instanceof SellFluidRecipe fluidRecipe) {
                ItemStack sellStack = ItemStack.EMPTY;
                if (packet.slotIndex != -1) {
                    sellStack = itemHandler.getStackInSlot(packet.slotIndex);
                    IFluidHandlerItem fluidHandler = sellStack.getCapability(Capabilities.FluidHandler.ITEM);
                    if (fluidHandler == null) { return; }
                    int targetAmount = (packet.quantity > 0) ? fluidRecipe.getFluid().getAmount() * packet.quantity : fluidRecipe.getFluid().getAmount();
                    boolean found = false;
                    for (int j = 0; j < fluidHandler.getTanks(); j++) {
                        FluidStack fluidStack = fluidHandler.getFluidInTank(j);
                        if (!fluidStack.isEmpty() && RecipeManager.matches(fluidStack, fluidRecipe.getFluid()) && fluidStack.getAmount() >= targetAmount) {
                            found = true;
                            break;
                        }
                    }
                    if (!found) { return; }
                } else {
                    int targetAmount = (packet.quantity > 0) ? fluidRecipe.getFluid().getAmount() * packet.quantity : fluidRecipe.getFluid().getAmount();
                    for (int i = 0; i < itemHandler.getSlots(); i++) {
                        ItemStack currentStack = itemHandler.getStackInSlot(i);
                        IFluidHandlerItem fluidHandler = currentStack.getCapability(Capabilities.FluidHandler.ITEM);
                        if (fluidHandler == null) { continue; }
                        for (int j = 0; j < fluidHandler.getTanks(); j++) {
                            FluidStack fluidStack = fluidHandler.getFluidInTank(j);
                            if (!fluidStack.isEmpty() && RecipeManager.matches(fluidStack, fluidRecipe.getFluid()) && fluidStack.getAmount() >= targetAmount) {
                                packet.slotIndex = i;
                                sellStack = currentStack;
                                break;
                            }
                        }
                        if (!sellStack.isEmpty()) { break; }
                    }
                }

                if (sellStack.isEmpty() || packet.slotIndex == -1) {
                    AdminShop.LOGGER.debug("Could not find fluid in inventory");
                    return;
                }

                IFluidHandlerItem fluidHandler = sellStack.getCapability(Capabilities.FluidHandler.ITEM);
                assert fluidHandler != null;
                int tankNumber = -1;
                int targetAmount = (packet.quantity > 0) ? fluidRecipe.getFluid().getAmount() * packet.quantity : fluidRecipe.getFluid().getAmount();
                for (int i = 0; i < fluidHandler.getTanks(); i++) {
                    FluidStack fluidStack = fluidHandler.getFluidInTank(i);
                    if (!fluidStack.isEmpty() && RecipeManager.matches(fluidStack, fluidRecipe.getFluid()) && fluidStack.getAmount() >= targetAmount) {
                        tankNumber = i;
                        break;
                    }
                }
                if (packet.quantity <= 0) { packet.quantity = 1; }

                if (tankNumber == -1) {
                    AdminShop.LOGGER.debug("Not enough fluid to sell");
                    return;
                }

                packet.sellFluid(player, level, itemHandler, packet.slotIndex, fluidRecipe, packet.quantity);
            }
        });
    }

    private void sellItem(ServerPlayer player, ServerLevel level, IItemHandler itemHandler, int slotIndex, SellItemRecipe recipe, int sellQuantity) {
        int quantity = recipe.getCount() * sellQuantity;
        int numSold = itemHandler.extractItem(slotIndex, quantity, false).getCount();
        long price = sellQuantity * recipe.getPrice();
        if (numSold != quantity) {
            AdminShop.LOGGER.debug("Target quantity and extracted value don't match: {}, {}", quantity, numSold);
            return;
        }
        MoneyHelper.get(level).addMoney(teamId, price);
    }

    private void sellFluid(ServerPlayer player, ServerLevel level, IItemHandler itemHandler, int slotIndex, SellFluidRecipe recipe, int sellQuantity) {
        ItemStack toExtract = itemHandler.getStackInSlot(slotIndex);
        IFluidHandlerItem fluidHandler = toExtract.getCapability(Capabilities.FluidHandler.ITEM);
        assert fluidHandler != null;
        int quantity = sellQuantity * recipe.getCount();
        FluidStack toDrain = recipe.getFluid().copy();
        toDrain.setAmount(quantity);
        FluidStack drained = fluidHandler.drain(toDrain, IFluidHandler.FluidAction.EXECUTE);
        long price = sellQuantity * recipe.getPrice();
        if (drained.getAmount() != quantity) {
            AdminShop.LOGGER.debug("Target quantity and extracted value don't match: {}, {}", quantity, drained.getAmount());
            return;
        }
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

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
