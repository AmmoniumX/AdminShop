package com.ammonium.adminshop.network;

import com.ammonium.adminshop.AdminShop;
import com.ammonium.adminshop.blocks.entity.SellerEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class PacketSetSellerRecipe implements CustomPacketPayload {

    public static final Type<PacketSetSellerRecipe> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(AdminShop.MODID, "set_seller_recipe"));
    public static final StreamCodec<FriendlyByteBuf, PacketSetSellerRecipe> STREAM_CODEC = StreamCodec.of(
            (buf, pkt) -> {
                buf.writeBlockPos(pkt.pos);
                buf.writeResourceLocation(pkt.recipeId);
            },
            buf -> new PacketSetSellerRecipe(buf.readBlockPos(), buf.readResourceLocation())
    );

    private final BlockPos pos;
    private final ResourceLocation recipeId;

    public PacketSetSellerRecipe(BlockPos pos, ResourceLocation recipeId) {
        this.pos = pos;
        this.recipeId = recipeId;
    }

    public static void handle(PacketSetSellerRecipe packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            if (!player.isCreative()) {
                AdminShop.LOGGER.error("Only creative-mode players can lock a seller's recipe");
                return;
            }
            AdminShop.LOGGER.debug("Locking seller recipe for " + packet.pos + " to " + packet.recipeId);
            ServerLevel level = player.serverLevel();
            BlockEntity blockEntity = level.getBlockEntity(packet.pos);
            if (!(blockEntity instanceof SellerEntity sellerEntity)) {
                AdminShop.LOGGER.error("BlockEntity at pos is not a SellerEntity");
                return;
            }
            sellerEntity.setLockedRecipeId(packet.recipeId);
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
