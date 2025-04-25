package com.ammonium.adminshop.recipes;

import com.ammonium.adminshop.AdminShop;
import com.ammonium.adminshop.blocks.ItemSellerMachine;
import com.ammonium.adminshop.money.BankAccount;
import com.ammonium.adminshop.money.MoneyManager;
import com.google.gson.JsonObject;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;

public class ShopSellItemRecipe implements Recipe<Container> {
    private final ResourceLocation id;
    private final String permit;
    private final ItemStack item;
    private final long price;

    public ShopSellItemRecipe(ResourceLocation id, long price, ItemStack item, String permit) {
        this.id = id;
        this.price = price;
        this.item = item;
        this.permit = permit != null ? permit : "";
    }

    public boolean matches(ServerLevel level, ItemSellerMachine machine) {

        // Get account information from server side
        Pair<String, Integer> account = machine.getAccount();
        if (account == null) {
            AdminShop.LOGGER.debug("ShopBuyItemRecipe.matches: account is null");
            return false;
        }
        BankAccount bankAccount = MoneyManager.get(level).getBankAccount(account);
        if (bankAccount == null) {
            AdminShop.LOGGER.debug("ShopBuyItemRecipe.matches: bankAccount is null");
            return false;
        }

        // Check permit status
        if ((!permit.isEmpty()) && (!bankAccount.hasPermit(Integer.parseInt(permit)))) { // TODO switch permits to strings
            AdminShop.LOGGER.debug("ShopBuyItemRecipe.matches: account does not have permit {}", permit);
            return false;
        }

        // Check if machine contains at least said number of items
        IItemHandler handler = machine.getCapability(ForgeCapabilities.ITEM_HANDLER).orElse(null);
        if (handler == null) {
            AdminShop.LOGGER.debug("ShopBuyItemRecipe.matches: handler is null");
            return false;
        }
        boolean hasItem = false;
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            if (!handler.extractItem(slot, item.getCount(), true).isEmpty()) {
                hasItem = true;
                break;
            }
        }
        return true;
    }

    public void sell(ServerLevel level, ItemSellerMachine machine) {
        // Get account information from server side
        // Important: we assume that this is only ever called after matches() succeeds
        MoneyManager manager = MoneyManager.get(level);
        Pair<String, Integer> account = machine.getAccount();
        IItemHandler handler = machine.getCapability(ForgeCapabilities.ITEM_HANDLER).orElse(null);

        for (int slot = 0; slot < handler.getSlots(); slot++) {
            if (!handler.extractItem(slot, item.getCount(), false).isEmpty()) {
                manager.addBalance(account, price);
                break;
            }
        }
        throw new IllegalStateException("ShopSellItemRecipe.sell: item not found in machine after matches");
    }

    @Override
    public boolean matches(Container container, Level level) {
        return false;
    }

    @Override
    public @NotNull ItemStack assemble(Container container) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canCraftInDimensions(int i, int i1) {
        return false;
    }

    @Override
    public @NotNull ItemStack getResultItem() {
        return ItemStack.EMPTY;
    }

    @Override
    public ResourceLocation getId() {
        return id;
    }

    @Override
    public RecipeSerializer<?> getSerializer() { // TODO
        return null;
    }

    @Override
    public RecipeType<?> getType() { // TODO
        return null;
    }

    public static class Serializer implements RecipeSerializer<ShopSellItemRecipe> {

        public @NotNull ShopSellItemRecipe fromJson(@NotNull ResourceLocation id, @NotNull JsonObject json) {
            long price = GsonHelper.getAsLong(json, "price");
            ItemStack item = ShapedRecipe.itemStackFromJson(GsonHelper.getAsJsonObject(json, "item"));
            String permit = GsonHelper.getAsString(json, "permit", "");
            return new ShopSellItemRecipe(id, price, item, permit);
        }

        public ShopSellItemRecipe fromNetwork(@NotNull ResourceLocation id, FriendlyByteBuf buffer) {
            long price = buffer.readLong();
            ItemStack result = buffer.readItem();
            String permit = buffer.readUtf();
            return new ShopSellItemRecipe(id, price, result, permit);
        }

        public void toNetwork(FriendlyByteBuf buffer, ShopSellItemRecipe pRecipe) {
            buffer.writeLong(pRecipe.price);
            buffer.writeItem(pRecipe.item);
            buffer.writeUtf(pRecipe.permit);
        }
    }
}
