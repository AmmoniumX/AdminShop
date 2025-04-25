package com.ammonium.adminshop.recipes;

import com.ammonium.adminshop.AdminShop;
import com.ammonium.adminshop.blocks.FluidBuyerMachine;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;

public class ShopBuyFluidRecipe implements Recipe<Container> {
    private final ResourceLocation id;
    private final long price;
    private final FluidStack fluid;
    private final String permit;

    public ShopBuyFluidRecipe(ResourceLocation id, long price, FluidStack fluid, String permit) {
        this.id = id;
        this.price = price;
        this.fluid = fluid;
        this.permit = permit != null ? permit : "";
    }

    public boolean matches(ServerLevel level, FluidBuyerMachine machine) {
        // Get account information from server side
        Pair<String, Integer> account = machine.getAccount();
        if (account == null) {
            AdminShop.LOGGER.debug("ShopBuyFluidRecipe: account is null");
            return false;
        }
        BankAccount bankAccount = MoneyManager.get(level).getBankAccount(account);
        if (bankAccount == null) {
            AdminShop.LOGGER.debug("ShopBuyFluidRecipe: bankAccount is null");
            return false;
        }

        // Check permit status
        if ((!permit.isEmpty()) && (!bankAccount.hasPermit(Integer.parseInt(permit)))) { // TODO switch permits to strings
            AdminShop.LOGGER.debug("ShopBuyFluidRecipe: account does not have permit {}", permit);
            return false;
        }
        // Check account balance
        if (bankAccount.getBalance() < price) {
            AdminShop.LOGGER.debug("ShopBuyFluidRecipe: account does not have enough money");
            return false;
        }

        // Check if machine can hold result fluid
        LazyOptional<IFluidHandler> lazyHandler = machine.getCapability(ForgeCapabilities.FLUID_HANDLER);
        if (!lazyHandler.isPresent()) {
            AdminShop.LOGGER.debug("ShopBuyFluidRecipe: machine does not have fluid handler");
            return false;
        }
        IFluidHandler handler = lazyHandler.orElseThrow(IllegalStateException::new);
        if (handler.fill(fluid, IFluidHandler.FluidAction.SIMULATE) < fluid.getAmount()) {
            AdminShop.LOGGER.debug("ShopBuyFluidRecipe: machine cannot hold result fluid");
            return false;
        }
        return true;
    }

    public FluidStack buy(ServerLevel level, FluidBuyerMachine machine) {
        // Get account information from server side
        // Important: we assume that this is only ever called after matches() succeeds
        MoneyManager manager = MoneyManager.get(level);
        Pair<String, Integer> account = machine.getAccount();
        manager.subtractBalance(account, price);
        IFluidHandler handler = machine.getCapability(ForgeCapabilities.FLUID_HANDLER)
                .orElseThrow(IllegalStateException::new);
        FluidStack output = fluid.copy();
        handler.fill(output, IFluidHandler.FluidAction.EXECUTE);
        return output;
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
        return null;
    }

    @Override
    public RecipeType<?> getType() {
        return null;
    }

    public static class Serializer implements RecipeSerializer<ShopBuyFluidRecipe> {

        public ShopBuyFluidRecipe fromJson(ResourceLocation id, JsonObject json) {
            long price = GsonHelper.getAsLong(json, "price");
            ResourceLocation fluidId = new ResourceLocation(GsonHelper.getAsString(json, "result"));
            int amount = GsonHelper.getAsInt(json, "amount", 1000);
            String permit = GsonHelper.getAsString(json, "permit", "");

            Fluid fluid = ForgeRegistries.FLUIDS.getValue(fluidId);
            if (fluid == null) { throw new IllegalArgumentException("Unknown fluid: " + fluidId); }
            FluidStack result = new FluidStack(fluid, amount);

            return new ShopBuyFluidRecipe(id, price, result, permit);
        }

        public ShopBuyFluidRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buffer) {
            long price = buffer.readLong();
            ResourceLocation fluidId = buffer.readResourceLocation();
            int amount = buffer.readInt();
            String permit = buffer.readUtf();

            Fluid fluid = ForgeRegistries.FLUIDS.getValue(fluidId);
            if (fluid == null) { throw new IllegalArgumentException("Unknown fluid: " + fluidId); }
            FluidStack result = new FluidStack(fluid, amount);

            return new ShopBuyFluidRecipe(id, price, result, permit);
        }

        public void toNetwork(FriendlyByteBuf buffer, ShopBuyFluidRecipe recipe) {
            buffer.writeLong(recipe.price);
            ResourceLocation fluidId = ForgeRegistries.FLUIDS.getKey(recipe.fluid.getFluid());
            if (fluidId == null) { throw new IllegalArgumentException("Unknown fluid: " + recipe.fluid.getFluid()); }
            buffer.writeResourceLocation(fluidId);
            buffer.writeInt(recipe.fluid.getAmount());
            buffer.writeUtf(recipe.permit);
        }
    }
}
