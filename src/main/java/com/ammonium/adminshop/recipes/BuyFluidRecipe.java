package com.ammonium.adminshop.recipes;

import com.ammonium.adminshop.AdminShop;
import com.ammonium.adminshop.blocks.interfaces.FluidBuyerMachine;
import com.ammonium.adminshop.money.MoneyHelper;
import com.ammonium.adminshop.recipes.interfaces.BuyRecipe;
import com.ammonium.adminshop.recipes.interfaces.FluidRecipe;
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
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jetbrains.annotations.NotNull;

public class BuyFluidRecipe implements BuyRecipe, FluidRecipe {
    private final ResourceLocation id;
    private final long price;
    private final FluidStack fluid;
    private final String permit;

    public BuyFluidRecipe(ResourceLocation id, long price, FluidStack fluid, String permit) {
        this.id = id;
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
        // Get account information from server side
        // Important: we assume that this is only ever called after matches() succeeds
        MoneyHelper.MoneyAccount account = MoneyHelper.get(level).getAccountById(machine.getTeamId());
        MoneyHelper.get(level).removeMoney(account.teamId(), price);
        return fluid.copy();
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
    public ResourceLocation getId() {
        return id;
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

        public BuyFluidRecipe fromJson(ResourceLocation id, JsonObject json) {
            long price = GsonHelper.getAsLong(json, "price");
            JsonObject resultJson = GsonHelper.getAsJsonObject(json, "result");
            ResourceLocation fluidId = new ResourceLocation(GsonHelper.getAsString(resultJson, "fluid"));
            int amount = GsonHelper.getAsInt(resultJson, "amount", 1000);
            String permit = GsonHelper.getAsString(json, "permit", "");

            Fluid fluid = ForgeRegistries.FLUIDS.getValue(fluidId);
            if (fluid == null) { throw new IllegalArgumentException("Unknown fluid: " + fluidId); }
            FluidStack result = new FluidStack(fluid, amount);

            return new BuyFluidRecipe(id, price, result, permit);
        }

        public BuyFluidRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buffer) {
            long price = buffer.readLong();
            ResourceLocation fluidId = buffer.readResourceLocation();
            int amount = buffer.readInt();
            String permit = buffer.readUtf();

            Fluid fluid = ForgeRegistries.FLUIDS.getValue(fluidId);
            if (fluid == null) { throw new IllegalArgumentException("Unknown fluid: " + fluidId); }
            FluidStack result = new FluidStack(fluid, amount);

            return new BuyFluidRecipe(id, price, result, permit);
        }

        public void toNetwork(FriendlyByteBuf buffer, BuyFluidRecipe recipe) {
            buffer.writeLong(recipe.price);
            ResourceLocation fluidId = ForgeRegistries.FLUIDS.getKey(recipe.fluid.getFluid());
            if (fluidId == null) { throw new IllegalArgumentException("Unknown fluid: " + recipe.fluid.getFluid()); }
            buffer.writeResourceLocation(fluidId);
            buffer.writeInt(recipe.fluid.getAmount());
            buffer.writeUtf(recipe.permit);
        }
    }
}
