package com.ammonium.adminshop.network;

import com.ammonium.adminshop.AdminShop;
import com.ammonium.adminshop.blocks.interfaces.FluidBuyerMachine;
import com.ammonium.adminshop.money.MoneyHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PacketSetFluidBuyerRecipe {
    private final BlockPos pos;
    private final ResourceLocation recipeId;

    public PacketSetFluidBuyerRecipe(BlockPos pos, ResourceLocation recipeId) {
        this.pos = pos;
        this.recipeId = recipeId;
    }

    public PacketSetFluidBuyerRecipe(FriendlyByteBuf buf) {
        this.pos = buf.readBlockPos();
        this.recipeId = buf.readResourceLocation();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBlockPos(this.pos);
        buf.writeResourceLocation(this.recipeId);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier){
        NetworkEvent.Context ctx = supplier.get();
        ctx.enqueueWork(() -> {
            //Client side accessed here
            //Do NOT call client-only code though, since server needs to access this too

            // Change machine's account
            ServerPlayer player = ctx.getSender();

            if (player != null) {
                AdminShop.LOGGER.debug("Setting buyer recipe for "+this.pos+" to "+this.recipeId);
                ServerLevel level = ctx.getSender().getLevel();
                BlockEntity blockEntity = level.getBlockEntity(this.pos);
                if (!(blockEntity instanceof FluidBuyerMachine buyerEntity)) {
                    AdminShop.LOGGER.error("BlockEntity at pos is not FluidBuyerMachine");
                    return;
                }

                // Check if player has access to the machine's account
                if (!MoneyHelper.get(level).isMemberOfTeam(buyerEntity.getTeamId(), player)) {
                    AdminShop.LOGGER.error("Player does not have access to this machine's account");
                    return;
                }
                // Apply changes to buyerEntity
                buyerEntity.setRecipe(this.recipeId);
            }
        });
        return true;
    }
}
