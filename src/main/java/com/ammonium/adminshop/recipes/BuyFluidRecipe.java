package com.ammonium.adminshop.recipes;

import com.ammonium.adminshop.AdminShop;
import com.ammonium.adminshop.blocks.interfaces.FluidBuyerMachine;
import com.ammonium.adminshop.money.MoneyHelper;
import com.ammonium.adminshop.recipes.interfaces.BuyRecipe;
import com.ammonium.adminshop.recipes.interfaces.FluidRecipe;
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
import net.neoforged.neoforge.fluids.FluidStack;
import org.jetbrains.annotations.NotNull;

public class BuyFluidRecipe implements BuyRecipe, FluidRecipe {
    private final long price;
    private final FluidStack fluid;
    private final String permit;

    public BuyFluidRecipe(long price, FluidStack fluid, String permit) {
        this.price = price;
        this.fluid = fluid;
        this.permit = permit != null ? permit : "";
    }

    public boolean matches(MoneyHelper.MoneyAccount account, FluidBuyerMachine machine) {

        if (account == null) {
            AdminShop.LOGGER.debug("ShopBuyFluidRecipe: account is null");
            return false;
        }

        // Check permit status
        if ((!permit.isEmpty()) && (!MoneyHelper.hasPermit(account, permit))) {
            AdminShop.LOGGER.debug("ShopBuyFluidRecipe: account does not have permit {}", permit);
            return false;
        }
        // Check account balance
        if (account.balance() < price) {
            AdminShop.LOGGER.debug("ShopBuyFluidRecipe: account does not have enough money");
            return false;
        }

        return true;
    }

    public String getPermit() {
        return permit;
    }

    public String getPermitTranslationKey() {
        return "adminshop.permit." + permit;
    }

    public long getPrice() {
        return price;
    }

    public FluidStack getFluid() {
        return fluid.copy();
    }

    public int getCount() {
        return fluid.getAmount();
    }

    public Component getName() {
        return fluid.getDisplayName();
    }

    @Override
    public String getSearchTerm() {
        return getName().getString().toLowerCase().strip();
    }

    public FluidStack buy(ServerLevel level, FluidBuyerMachine machine) {
        MoneyHelper.MoneyAccount account = MoneyHelper.get(level).getAccountById(machine.getTeamId());
        MoneyHelper.get(level).removeMoney(account.teamId(), price);
        return fluid.copy();
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
    public RecipeSerializer<?> getSerializer() {
        return ModRecipeSerializers.SHOP_BUY_FLUID_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipeTypes.SHOP_BUY_FLUID.get();
    }

    public static class Serializer implements RecipeSerializer<BuyFluidRecipe> {
        private static final MapCodec<BuyFluidRecipe> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                Codec.LONG.fieldOf("price").forGetter(r -> r.price),
                FluidStack.CODEC.fieldOf("result").forGetter(r -> r.fluid),
                Codec.STRING.optionalFieldOf("permit", "").forGetter(r -> r.permit)
            ).apply(instance, BuyFluidRecipe::new)
        );

        private static final StreamCodec<RegistryFriendlyByteBuf, BuyFluidRecipe> STREAM_CODEC = StreamCodec.of(
            (buf, r) -> {
                buf.writeLong(r.price);
                FluidStack.STREAM_CODEC.encode(buf, r.fluid);
                buf.writeUtf(r.permit);
            },
            buf -> {
                long price = buf.readLong();
                FluidStack fluid = FluidStack.STREAM_CODEC.decode(buf);
                String permit = buf.readUtf();
                return new BuyFluidRecipe(price, fluid, permit);
            }
        );

        @Override
        public MapCodec<BuyFluidRecipe> codec() { return CODEC; }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, BuyFluidRecipe> streamCodec() { return STREAM_CODEC; }
    }
}
