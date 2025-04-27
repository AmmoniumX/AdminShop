package com.ammonium.adminshop.recipes;

import com.ammonium.adminshop.AdminShop;
import com.ammonium.adminshop.blocks.ItemBuyerMachine;
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
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;

public class BuyItemRecipe implements Recipe<Container> {
    private final ResourceLocation id;
    private final long price;
    private final String permit;
    private final ItemStack result;

    public BuyItemRecipe(ResourceLocation id, long price, ItemStack result, String permit) {
        this.id = id;
        this.price = price;
        this.result = result;
        this.permit = permit != null ? permit : "";
    }

    public boolean matches(BankAccount account, ItemBuyerMachine machine) {

        // Check permit status
        if ((!permit.isEmpty()) && (!account.hasPermit(Integer.parseInt(permit)))) { // TODO switch permits to strings
            AdminShop.LOGGER.debug("ShopBuyItemRecipe.matches: account does not have permit {}", permit);
            return false;
        }
        // Check account balance
        if (account.getBalance() < price) {
            AdminShop.LOGGER.debug("ShopBuyItemRecipe.matches: account does not have enough money");
            return false;
        }

        return true;
    }

    public ItemStack getItem() {
        return result.copy();
    }

    public String getPermit() {
        return permit;
    }

    public ItemStack buy(ServerLevel level, ItemBuyerMachine machine) {
        // Get account information from server side
        // Important: we assume that this is only ever called after matches() succeeds
        MoneyManager manager = MoneyManager.get(level);
        Pair<String, Integer> account = machine.getAccountId();
        manager.subtractBalance(account, price);
        return result.copy();
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
    public RecipeSerializer<?> getSerializer() {
        return ModRecipeSerializers.SHOP_BUY_ITEM_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipeTypes.SHOP_BUY_ITEM.get();
    }

    public static class Serializer implements RecipeSerializer<BuyItemRecipe> {

        public @NotNull BuyItemRecipe fromJson(@NotNull ResourceLocation id, @NotNull JsonObject json) {
            long price = GsonHelper.getAsLong(json, "price");
            ItemStack item = ShapedRecipe.itemStackFromJson(GsonHelper.getAsJsonObject(json, "result"));
            String permit = GsonHelper.getAsString(json, "permit", "");
            return new BuyItemRecipe(id, price, item, permit);
        }

        public BuyItemRecipe fromNetwork(@NotNull ResourceLocation id, FriendlyByteBuf buffer) {
            long price = buffer.readLong();
            ItemStack result = buffer.readItem();
            String permit = buffer.readUtf();
            return new BuyItemRecipe(id, price, result, permit);
        }

        public void toNetwork(FriendlyByteBuf buffer, BuyItemRecipe pRecipe) {
            buffer.writeLong(pRecipe.price);
            buffer.writeItem(pRecipe.result);
            buffer.writeUtf(pRecipe.permit);
        }
    }

}
