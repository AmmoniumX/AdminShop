package com.ammonium.adminshop.network;

import com.ammonium.adminshop.AdminShop;
import com.ammonium.adminshop.blocks.interfaces.ItemBuyerMachine;
import com.ammonium.adminshop.money.MoneyHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class PacketSetItemBuyerRecipe implements CustomPacketPayload {

    public static final Type<PacketSetItemBuyerRecipe> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(AdminShop.MODID, "set_item_buyer_recipe"));
    public static final StreamCodec<FriendlyByteBuf, PacketSetItemBuyerRecipe> STREAM_CODEC = StreamCodec.of(
            (buf, pkt) -> {
                buf.writeBlockPos(pkt.pos);
                buf.writeResourceLocation(pkt.recipeId);
            },
            buf -> new PacketSetItemBuyerRecipe(buf.readBlockPos(), buf.readResourceLocation())
    );

    private final BlockPos pos;
    private final ResourceLocation recipeId;

    public PacketSetItemBuyerRecipe(BlockPos pos, ResourceLocation recipeId) {
        this.pos = pos;
        this.recipeId = recipeId;
    }

    public static void handle(PacketSetItemBuyerRecipe packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            AdminShop.LOGGER.debug("Setting buyer recipe for " + packet.pos + " to " + packet.recipeId);
            ServerLevel level = player.serverLevel();
            BlockEntity blockEntity = level.getBlockEntity(packet.pos);
            if (!(blockEntity instanceof ItemBuyerMachine buyerEntity)) {
                AdminShop.LOGGER.error("BlockEntity at pos is not BuyerMachine");
                return;
            }
            if (player.isCreative()) {
                buyerEntity.forceSetRecipe(packet.recipeId);
                return;
            }
            if (!MoneyHelper.get(level).isMemberOfTeam(buyerEntity.getTeamId(), player)) {
                AdminShop.LOGGER.error("Player does not have access to this machine's account");
                return;
            }
            buyerEntity.setRecipe(packet.recipeId);
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
