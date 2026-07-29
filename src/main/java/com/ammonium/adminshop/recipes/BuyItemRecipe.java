package com.ammonium.adminshop.recipes;

import com.ammonium.adminshop.AdminShop;
import com.ammonium.adminshop.blocks.interfaces.ItemBuyerMachine;
import com.ammonium.adminshop.money.MoneyHelper;
import com.ammonium.adminshop.recipes.interfaces.BuyRecipe;
import com.ammonium.adminshop.recipes.interfaces.ItemRecipe;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

public class BuyItemRecipe implements BuyRecipe, ItemRecipe {
    private final long price;
    private final String permit;
    private final ItemStack result;

    public BuyItemRecipe(long price, ItemStack result, String permit) {
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
        if ((!permit.isEmpty()) && !machine.bypassesPermits() && (!MoneyHelper.hasPermit(account, permit))) {
            return false;
        }
        // Check account balance
        if (account.balance() < price) {
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

    @Override
    public String getSearchTerm() {
        return getName().getString().toLowerCase().strip();
    }

    public ItemStack buy(ServerLevel level, ItemBuyerMachine machine) {
        MoneyHelper.MoneyAccount account = MoneyHelper.get(level).getAccountById(machine.getTeamId());
        MoneyHelper.get(level).removeMoney(account.teamId(), price);
        return result.copy();
    }

    @Override
    public boolean matches(RecipeInput input, Level level) {
        return false;
    }

    @Override
    public @NotNull ItemStack assemble(RecipeInput input, HolderLookup.Provider provider) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canCraftInDimensions(int i, int i1) {
        return false;
    }

    @Override
    public @NotNull ItemStack getResultItem(HolderLookup.Provider provider) {
        return ItemStack.EMPTY;
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
        public static final MapCodec<BuyItemRecipe> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                Codec.LONG.fieldOf("price").forGetter(r -> r.price),
                ItemStack.CODEC.fieldOf("result").forGetter(r -> r.result),
                Codec.STRING.optionalFieldOf("permit", "").forGetter(r -> r.permit)
            ).apply(instance, BuyItemRecipe::new)
        );

        public static final StreamCodec<RegistryFriendlyByteBuf, BuyItemRecipe> STREAM_CODEC = StreamCodec.of(
            (buf, r) -> {
                buf.writeLong(r.price);
                ItemStack.STREAM_CODEC.encode(buf, r.result);
                buf.writeUtf(r.permit);
            },
            buf -> {
                long price = buf.readLong();
                ItemStack result = ItemStack.STREAM_CODEC.decode(buf);
                String permit = buf.readUtf();
                return new BuyItemRecipe(price, result, permit);
            }
        );

        public Serializer() {
            AdminShop.LOGGER.info("[AdminShop] BuyItemRecipe.Serializer instantiated");
        }

        @Override
        public MapCodec<BuyItemRecipe> codec() {
            AdminShop.LOGGER.info("[AdminShop] BuyItemRecipe.Serializer.codec() called");
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, BuyItemRecipe> streamCodec() { return STREAM_CODEC; }
    }

}
