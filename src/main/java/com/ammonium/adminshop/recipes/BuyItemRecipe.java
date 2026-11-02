package com.ammonium.adminshop.recipes;

import com.ammonium.adminshop.AdminShop;
import com.ammonium.adminshop.blocks.interfaces.ItemBuyerMachine;
import com.ammonium.adminshop.money.MoneyHelper;
import com.ammonium.adminshop.recipes.interfaces.BuyRecipe;
import com.ammonium.adminshop.recipes.interfaces.ItemRecipe;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
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
    // Item stacks can't be created while recipes load, so the recipe keeps a template and creates the stack when first used
    private final ItemStackTemplate template;
    private ItemStack result;

    public BuyItemRecipe(long price, ItemStackTemplate result, String permit) {
        this.price = price;
        this.template = result;
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

    private ItemStack result() {
        if (result == null) {
            result = template.create();
        }
        return result;
    }

    public Optional<ItemStack> getItem() {
        return Optional.of(result().copy());
    }

    public ItemStack getDisplayItem() {
        return result().copy();
    }

    @Override
    public List<ItemStack> getValidItems() {
        return List.of(result().copy());
    }

    public int getCount() {
        return result().getCount();
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
        return result().getDisplayName();
    }

    @Override
    public String getSearchTerm() {
        return getName().getString().toLowerCase().strip();
    }

    public ItemStack buy(ServerLevel level, ItemBuyerMachine machine) {
        MoneyHelper.MoneyAccount account = MoneyHelper.get(level).getAccountById(machine.getTeamId());
        MoneyHelper.get(level).removeMoney(account.teamId(), price);
        return result().copy();
    }

    @Override
    public boolean matches(RecipeInput input, Level level) {
        return false;
    }

    @Override
    public @NotNull ItemStack assemble(RecipeInput input) {
        return ItemStack.EMPTY;
    }

    @Override
    public @NotNull RecipeSerializer<BuyItemRecipe> getSerializer() {
        return ModRecipeSerializers.SHOP_BUY_ITEM_SERIALIZER.get();
    }

    @Override
    public @NotNull RecipeType<BuyItemRecipe> getType() {
        return ModRecipeTypes.SHOP_BUY_ITEM.get();
    }

    public static class Serializer {
        public static final MapCodec<BuyItemRecipe> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                Codec.LONG.fieldOf("price").forGetter(r -> r.price),
                ItemStackTemplate.CODEC.fieldOf("result").forGetter(r -> r.template),
                Codec.STRING.optionalFieldOf("permit", "").forGetter(r -> r.permit)
            ).apply(instance, BuyItemRecipe::new)
        );

        public static final StreamCodec<RegistryFriendlyByteBuf, BuyItemRecipe> STREAM_CODEC = StreamCodec.of(
            (buf, r) -> {
                buf.writeLong(r.price);
                ItemStackTemplate.STREAM_CODEC.encode(buf, r.template);
                buf.writeUtf(r.permit);
            },
            buf -> {
                long price = buf.readLong();
                ItemStackTemplate result = ItemStackTemplate.STREAM_CODEC.decode(buf);
                String permit = buf.readUtf();
                return new BuyItemRecipe(price, result, permit);
            }
        );

        public Serializer() {
            AdminShop.LOGGER.info("[AdminShop] BuyItemRecipe.Serializer instantiated");
        }

        public MapCodec<BuyItemRecipe> codec() {
            AdminShop.LOGGER.info("[AdminShop] BuyItemRecipe.Serializer.codec() called");
            return CODEC;
        }

        public StreamCodec<RegistryFriendlyByteBuf, BuyItemRecipe> streamCodec() { return STREAM_CODEC; }
    }

}
