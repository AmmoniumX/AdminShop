package com.ammonium.adminshop.network;

import com.ammonium.adminshop.AdminShop;
import com.ammonium.adminshop.money.MoneyHelper;
import com.ammonium.adminshop.recipes.BuyFluidRecipe;
import com.ammonium.adminshop.recipes.BuyItemRecipe;
import com.ammonium.adminshop.recipes.interfaces.BuyRecipe;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.wrapper.PlayerMainInvWrapper;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.UUID;

public class PacketBuyRequest implements CustomPacketPayload {

    public static final Type<PacketBuyRequest> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(AdminShop.MODID, "buy_request"));
    public static final StreamCodec<FriendlyByteBuf, PacketBuyRequest> STREAM_CODEC = StreamCodec.of(
            (buf, pkt) -> {
                buf.writeUUID(pkt.teamId);
                buf.writeResourceLocation(pkt.recipeId);
                buf.writeInt(pkt.quantity);
            },
            buf -> new PacketBuyRequest(buf.readUUID(), buf.readResourceLocation(), buf.readInt())
    );

    private final int quantity;
    private final UUID teamId;
    private final ResourceLocation recipeId;

    public PacketBuyRequest(UUID teamId, ResourceLocation recipeId, int quantity) {
        this.teamId = teamId;
        this.recipeId = recipeId;
        this.quantity = quantity;
    }

    public static void handle(PacketBuyRequest packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            ServerLevel level = player.serverLevel();

            RecipeHolder<?> rawHolder = level.getRecipeManager().byKey(packet.recipeId).orElse(null);
            if (rawHolder == null || !(rawHolder.value() instanceof BuyRecipe recipe)) {
                AdminShop.LOGGER.debug("Not a valid BuyRecipe: {}", packet.recipeId);
                return;
            }

            AdminShop.LOGGER.debug("Performing buy transaction: ");
            if (recipe instanceof BuyItemRecipe itemRecipe) {
                AdminShop.LOGGER.debug("Item: {}", itemRecipe.getItem());
            } else if (recipe instanceof BuyFluidRecipe fluidRecipe) {
                AdminShop.LOGGER.debug("Fluid: {}", fluidRecipe.getFluid());
            }

            if (!MoneyHelper.get(level).hasPermit(packet.teamId, recipe.getPermit())) {
                AdminShop.LOGGER.debug("Account {} does not have permit {}", packet.teamId, recipe.getPermit());
                player.sendSystemMessage(Component.translatable("message.adminshop.no_permit", recipe.getPermit()));
                return;
            }

            if (recipe instanceof BuyItemRecipe itemRecipe) {
                packet.buyItemTransaction(player, level, itemRecipe, packet.quantity);
            } else if (recipe instanceof BuyFluidRecipe fluidRecipe) {
                packet.buyFluidTransaction(player, level, fluidRecipe, packet.quantity);
            } else {
                AdminShop.LOGGER.debug("Not a valid BuyRecipe: {}", packet.recipeId);
            }
        });
    }

    private void buyItemTransaction(ServerPlayer player, ServerLevel level, BuyItemRecipe recipe, int buyQuantity) {
        Inventory playerInventory = player.getInventory();
        IItemHandler iItemHandler = new PlayerMainInvWrapper(playerInventory);
        int quantity = buyQuantity * recipe.getCount();
        ItemStack toInsert = recipe.getItem().get();
        toInsert.setCount(quantity);
        ItemStack returned = ItemHandlerHelper.insertItemStacked(iItemHandler, toInsert, true);
        if (returned.getCount() == quantity) {
            player.sendSystemMessage(Component.translatable("message.adminshop.not_enough_space"));
        }
        long price = buyQuantity * recipe.getPrice();
        boolean success = MoneyHelper.get(level).removeMoney(teamId, price);
        if (success) {
            ItemHandlerHelper.insertItemStacked(iItemHandler, toInsert, false);
        } else {
            player.sendSystemMessage(Component.translatable("message.adminshop.not_enough_money"));
            AdminShop.LOGGER.debug("Not enough money in account to perform transaction.");
        }
    }

    private void buyFluidTransaction(ServerPlayer player, ServerLevel level, BuyFluidRecipe recipe, int buyQuantity) {
        AdminShop.LOGGER.debug("buyFluidTransaction: {}", buyQuantity);
        Inventory playerInventory = player.getInventory();
        IItemHandler playerInventoryHandler = new PlayerMainInvWrapper(playerInventory);

        FluidStack toInsert = recipe.getFluid().copy();
        int quantity = buyQuantity * recipe.getCount();
        toInsert.setAmount(quantity);
        int fillableContainerIdx = getFillableFluidContainer(playerInventoryHandler, toInsert.getFluid(), quantity);
        if (fillableContainerIdx == -1) {
            player.sendSystemMessage(Component.translatable("message.adminshop.no_container"));
            AdminShop.LOGGER.debug("No container found for fluid.");
            return;
        }
        ItemStack container = playerInventoryHandler.getStackInSlot(fillableContainerIdx);
        if (container.getItem().equals(Items.BUCKET) && container.getCount() != 1) {
            if (!hasEmptySlot(playerInventoryHandler)) {
                player.sendSystemMessage(Component.translatable("message.adminshop.no_container"));
                AdminShop.LOGGER.debug("Trying to fill into a bucket, but wouldn't have space for filled bucket");
                return;
            }
            ItemStack singleStack = container.copy();
            singleStack.setCount(1);
            container = singleStack;
        }
        IFluidHandlerItem containerHandler = container.getCapability(Capabilities.FluidHandler.ITEM);
        if (containerHandler == null) {
            AdminShop.LOGGER.debug("No fluid handler found for container");
            return;
        }
        int filledQuantity = containerHandler.fill(toInsert, IFluidHandler.FluidAction.SIMULATE);
        if (filledQuantity != quantity) {
            AdminShop.LOGGER.debug("Not enough space in container for fluid: {} / {}", filledQuantity, quantity);
            return;
        }
        toInsert.setAmount(filledQuantity);
        long price = recipe.getPrice() * buyQuantity;
        boolean success = MoneyHelper.get(level).removeMoney(teamId, price);
        if (success) {
            AdminShop.LOGGER.debug("Attempt to fill with {}, {}", toInsert.getHoverName().getString(), toInsert.getAmount());
            int filled = containerHandler.fill(toInsert, IFluidHandler.FluidAction.EXECUTE);
            AdminShop.LOGGER.debug("Filled with {} mb", filled);
            ItemStack newContainer = containerHandler.getContainer();
            AdminShop.LOGGER.debug("New container: {}", newContainer);
            if (!newContainer.equals(container)) {
                AdminShop.LOGGER.debug("Removing old container");
                playerInventoryHandler.extractItem(fillableContainerIdx, 1, false);
                AdminShop.LOGGER.debug("Giving new container");
                ItemStack inserted = ItemHandlerHelper.insertItemStacked(playerInventoryHandler, newContainer, false);
                if (inserted.getCount() != 0) {
                    player.sendSystemMessage(Component.translatable("message.adminshop.not_enough_space"));
                    AdminShop.LOGGER.debug("Error inserting fluid container: {}", inserted.getCount());
                }
            }
        } else {
            player.sendSystemMessage(Component.translatable("message.adminshop.not_enough_money"));
            AdminShop.LOGGER.debug("Not enough money in account to perform transaction.");
        }
    }

    public static int getFillableFluidContainer(IItemHandler itemHandler, Fluid fluid, int quantity) {
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            ItemStack stack = itemHandler.getStackInSlot(i);
            if (stack.isEmpty()) { continue; }
            if (stack.getItem().equals(Items.BUCKET) && quantity == 1000) {
                if (stack.getCount() > 1 && !hasEmptySlot(itemHandler)) {
                    AdminShop.LOGGER.debug("Trying to fill into stacked buckets, but wouldn't have space. Skipping");
                    continue;
                }
                Item bucketItem = fluid.getBucket();
                if (bucketItem != null && bucketItem != Items.AIR) { return i; }
            }
            IFluidHandlerItem fluidHandlerCapability = stack.getCapability(Capabilities.FluidHandler.ITEM);
            if (fluidHandlerCapability == null) { continue; }
            int canFillAmount = fluidHandlerCapability.fill(new FluidStack(fluid, quantity), IFluidHandler.FluidAction.SIMULATE);
            if (canFillAmount > 0) { return i; }
        }
        return -1;
    }

    public static boolean hasEmptySlot(IItemHandler itemHandler) {
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            if (itemHandler.getStackInSlot(i).isEmpty()) { return true; }
        }
        return false;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
