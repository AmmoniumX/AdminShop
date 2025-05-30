package com.ammonium.adminshop.recipes;

import com.ammonium.adminshop.AdminShop;
import com.ammonium.adminshop.blocks.interfaces.ItemBuyerMachine;
import com.ammonium.adminshop.money.MoneyHelper;
import com.ammonium.adminshop.recipes.interfaces.BuyRecipe;
import com.ammonium.adminshop.recipes.interfaces.ItemRecipe;
import com.google.gson.JsonObject;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.crafting.CraftingHelper;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

public class BuyItemRecipe implements BuyRecipe, ItemRecipe {
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

    public boolean matches(MoneyHelper.MoneyAccount account, ItemBuyerMachine machine) {

        if (account == null) {
            AdminShop.LOGGER.debug("ShopBuyItemRecipe.matches: account is null");
            return false;
        }

        // Check permit status
        if ((!permit.isEmpty()) && (!MoneyHelper.hasPermit(account, permit))) {
//            AdminShop.LOGGER.debug("ShopBuyItemRecipe.matches: account does not have permit {}", permit);
            return false;
        }
        // Check account balance
        if (account.balance() < price) {
//            AdminShop.LOGGER.debug("ShopBuyItemRecipe.matches: account does not have enough money");
            return false;
        }
        return true;
    }

    public Optional<ItemStack> getItem() {
        return Optional.of(result.copy());
    }

    public ItemStack getDisplayItem() {
        return result.copy();
    }

    @Override
    public List<ItemStack> getValidItems() {
        return List.of(result.copy());
    }

    public int getCount() {
        return result.getCount();
    }

    public long getPrice() {
        return price;
    }

    public String getPermit() {
        return permit;
    }

    public String getPermitTranslationKey() {
        return "adminshop.permit." + permit;
    }

    public Component getName() {
        return result.getDisplayName();
    }

    public ItemStack buy(ServerLevel level, ItemBuyerMachine machine) {
        // Get account information from server side
        // Important: we assume that this is only ever called after matches() succeeds
        MoneyHelper.MoneyAccount account = MoneyHelper.get(level).getAccountById(machine.getTeamId());
        MoneyHelper.get(level).removeMoney(account.teamId(), price);
        return result.copy();
    }

    @Override
    public boolean matches(Container container, Level level) {
        return false;
    }

    @Override
    public @NotNull ItemStack assemble(Container container, RegistryAccess registryAccess) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canCraftInDimensions(int i, int i1) {
        return false;
    }

    @Override
    public @NotNull ItemStack getResultItem(RegistryAccess registryAccess) {
        return ItemStack.EMPTY;
    }

    @Override
    public @NotNull ResourceLocation getId() {
        return id;
    }

    @Override
    public @NotNull RecipeSerializer<?> getSerializer() {
        return ModRecipeSerializers.SHOP_BUY_ITEM_SERIALIZER.get();
    }

    @Override
    public @NotNull RecipeType<?> getType() {
        return ModRecipeTypes.SHOP_BUY_ITEM.get();
    }

    public static class Serializer implements RecipeSerializer<BuyItemRecipe> {

        public @NotNull BuyItemRecipe fromJson(@NotNull ResourceLocation id, @NotNull JsonObject json) {
            long price = GsonHelper.getAsLong(json, "price");
            ItemStack item = CraftingHelper.getItemStack(GsonHelper.getAsJsonObject(json, "result"), true, true);
            if (item.getCount() > item.getMaxStackSize()) {
                AdminShop.LOGGER.warn("ItemStack count {} exceeds max stack size {} for item {}", item.getCount(), item.getMaxStackSize(), item.getItem());
                item.setCount(item.getMaxStackSize());
            }
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
