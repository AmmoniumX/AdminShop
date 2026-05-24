package com.ammonium.adminshop.recipes;

import com.ammonium.adminshop.AdminShop;
import com.ammonium.adminshop.blocks.entity.FluidHandlerBlockEntity;
import com.ammonium.adminshop.blocks.interfaces.FluidSellerMachine;
import com.ammonium.adminshop.money.MoneyHelper;
import com.ammonium.adminshop.recipes.interfaces.FluidRecipe;
import com.ammonium.adminshop.recipes.interfaces.SellRecipe;
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
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.NotNull;

public class SellFluidRecipe implements SellRecipe, FluidRecipe {
    private final long price;
    private final FluidStack fluid;
    private final String permit;

    public SellFluidRecipe(long price, FluidStack fluid, String permit) {
        this.price = price;
        this.fluid = fluid;
        this.permit = permit != null ? permit : "";
    }

    public boolean matches(MoneyHelper.MoneyAccount account, FluidSellerMachine machine) {

        if (account == null) {
            AdminShop.LOGGER.debug("ShopSellFluidRecipe: account is null");
            return false;
        }

        // Check permit status
        if ((!permit.isEmpty()) && (!MoneyHelper.hasPermit(account, permit))) {
            AdminShop.LOGGER.debug("ShopSellFluidRecipe: account does not have permit {}", permit);
            return false;
        }

        // Check if machine contains fluid
        if (!(machine instanceof FluidHandlerBlockEntity be)) {
            AdminShop.LOGGER.debug("ShopSellFluidRecipe: machine is not FluidHandlerBlockEntity");
            return false;
        }
        IFluidHandler handler = be.getTank();
        if (handler.drain(fluid.copy(), IFluidHandler.FluidAction.SIMULATE).getAmount() < fluid.getAmount()) {
            return false;
        }
        return true;
    }

    public FluidStack getFluid() {
        return fluid;
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

    public void sell(ServerLevel level, FluidSellerMachine machine) {
        MoneyHelper.get(level).addMoney(machine.getTeamId(), price);
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
        return ModRecipeSerializers.SHOP_SELL_FLUID_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipeTypes.SHOP_SELL_FLUID.get();
    }

    public static class Serializer implements RecipeSerializer<SellFluidRecipe> {
        private static final MapCodec<SellFluidRecipe> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                Codec.LONG.fieldOf("price").forGetter(r -> r.price),
                FluidStack.CODEC.fieldOf("result").forGetter(r -> r.fluid),
                Codec.STRING.optionalFieldOf("permit", "").forGetter(r -> r.permit)
            ).apply(instance, SellFluidRecipe::new)
        );

        private static final StreamCodec<RegistryFriendlyByteBuf, SellFluidRecipe> STREAM_CODEC = StreamCodec.of(
            (buf, r) -> {
                buf.writeLong(r.price);
                FluidStack.STREAM_CODEC.encode(buf, r.fluid);
                buf.writeUtf(r.permit);
            },
            buf -> {
                long price = buf.readLong();
                FluidStack fluid = FluidStack.STREAM_CODEC.decode(buf);
                String permit = buf.readUtf();
                return new SellFluidRecipe(price, fluid, permit);
            }
        );

        @Override
        public MapCodec<SellFluidRecipe> codec() { return CODEC; }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, SellFluidRecipe> streamCodec() { return STREAM_CODEC; }
    }
}
