package com.ammonium.adminshop.network;

import com.ammonium.adminshop.AdminShop;
import com.ammonium.adminshop.blocks.entity.SellerEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PacketSetSellerRecipe {
    private final BlockPos pos;
    private final ResourceLocation recipeId;

    public PacketSetSellerRecipe(BlockPos pos, ResourceLocation recipeId) {
        this.pos = pos;
        this.recipeId = recipeId;
    }

    public PacketSetSellerRecipe(FriendlyByteBuf buf) {
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
            ServerPlayer player = ctx.getSender();
            if (player == null) {
                return;
            }
            if (!player.isCreative()) {
                AdminShop.LOGGER.error("Only creative-mode players can lock a seller's recipe");
                return;
            }
            AdminShop.LOGGER.debug("Locking seller recipe for " + this.pos + " to " + this.recipeId);
            ServerLevel level = player.serverLevel();
            BlockEntity blockEntity = level.getBlockEntity(this.pos);
            if (!(blockEntity instanceof SellerEntity sellerEntity)) {
                AdminShop.LOGGER.error("BlockEntity at pos is not a SellerEntity");
                return;
            }
            sellerEntity.setLockedRecipeId(this.recipeId);
        });
        return true;
    }
}
