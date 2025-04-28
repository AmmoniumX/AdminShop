package com.ammonium.adminshop.recipes;

import com.ammonium.adminshop.AdminShop;
import com.ammonium.adminshop.blocks.interfaces.FluidSellerMachine;
import com.ammonium.adminshop.money.BankAccount;
import com.ammonium.adminshop.money.MoneyManager;
import com.ammonium.adminshop.recipes.interfaces.FluidRecipe;
import com.ammonium.adminshop.recipes.interfaces.SellRecipe;
import com.google.gson.JsonObject;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;

public class SellFluidRecipe implements SellRecipe, FluidRecipe {
    private final ResourceLocation id;
    private final long price;
    private final FluidStack fluid;
    private final String permit;

    public SellFluidRecipe(ResourceLocation id, long price, FluidStack fluid, String permit) {
        this.id = id;
        this.price = price;
        this.fluid = fluid;
        this.permit = permit != null ? permit : "";
    }

    public boolean matches(BankAccount account, FluidSellerMachine machine) {

        // Check permit status
        if ((!permit.isEmpty()) && (!account.hasPermit(Integer.parseInt(permit)))) { // TODO switch permits to strings
            AdminShop.LOGGER.debug("ShopSellFluidRecipe: account does not have permit {}", permit);
            return false;
        }
        // Check account balance
        if (account.getBalance() < price) {
            AdminShop.LOGGER.debug("ShopSellFluidRecipe: account does not have enough money");
            return false;
        }

        // Check if machine contains fluid
        LazyOptional<IFluidHandler> lazyHandler = machine.getCapability(ForgeCapabilities.FLUID_HANDLER);
        if (!lazyHandler.isPresent()) {
            AdminShop.LOGGER.debug("ShopSellFluidRecipe: machine does not have fluid handler");
            return false;
        }
        IFluidHandler handler = lazyHandler.orElseThrow(IllegalStateException::new);
        if (handler.drain(fluid, IFluidHandler.FluidAction.SIMULATE).getAmount() < fluid.getAmount()) {
            AdminShop.LOGGER.debug("ShopSellFluidRecipe: machine does not have enough fluid");
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

    public int getCount() {
        return fluid.getAmount();
    }

    public String getName() {
        return fluid.getDisplayName().getString();
    }

    public void sell(ServerLevel level, FluidSellerMachine machine) {
        // Get account information from server side
        // Important: we assume that this is only ever called after matches() succeeds
        MoneyManager manager = MoneyManager.get(level);
        Pair<String, Integer> account = machine.getAccountId();
        manager.addBalance(account, price);
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
        return ModRecipeSerializers.SHOP_SELL_FLUID_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipeTypes.SHOP_SELL_FLUID.get();
    }

    public static class Serializer implements RecipeSerializer<SellFluidRecipe> {

        public SellFluidRecipe fromJson(ResourceLocation id, JsonObject json) {
            long price = GsonHelper.getAsLong(json, "price");
            JsonObject fluidJson = GsonHelper.getAsJsonObject(json, "fluid");
            ResourceLocation fluidId = new ResourceLocation(GsonHelper.getAsString(fluidJson, "fluid"));
            int amount = GsonHelper.getAsInt(fluidJson, "amount", 1000);
            String permit = GsonHelper.getAsString(json, "permit", "");

            Fluid fluid = ForgeRegistries.FLUIDS.getValue(fluidId);
            if (fluid == null) { throw new IllegalArgumentException("Unknown fluid: " + fluidId); }
            FluidStack result = new FluidStack(fluid, amount);

            return new SellFluidRecipe(id, price, result, permit);
        }

        public SellFluidRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buffer) {
            long price = buffer.readLong();
            ResourceLocation fluidId = buffer.readResourceLocation();
            int amount = buffer.readInt();
            String permit = buffer.readUtf();

            Fluid fluid = ForgeRegistries.FLUIDS.getValue(fluidId);
            if (fluid == null) { throw new IllegalArgumentException("Unknown fluid: " + fluidId); }
            FluidStack result = new FluidStack(fluid, amount);

            return new SellFluidRecipe(id, price, result, permit);
        }

        public void toNetwork(FriendlyByteBuf buffer, SellFluidRecipe recipe) {
            buffer.writeLong(recipe.price);
            ResourceLocation fluidId = ForgeRegistries.FLUIDS.getKey(recipe.fluid.getFluid());
            if (fluidId == null) { throw new IllegalArgumentException("Unknown fluid: " + recipe.fluid.getFluid()); }
            buffer.writeResourceLocation(fluidId);
            buffer.writeInt(recipe.fluid.getAmount());
            buffer.writeUtf(recipe.permit);
        }
    }
}
