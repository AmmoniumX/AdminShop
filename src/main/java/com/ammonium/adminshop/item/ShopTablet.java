package com.ammonium.adminshop.item;

import com.ammonium.adminshop.money.MoneyHelper;
import com.ammonium.adminshop.network.PacketSyncMoneyToClient;
import com.ammonium.adminshop.screen.ShopMenu;
import com.ammonium.adminshop.setup.Messages;
import com.ammonium.adminshop.setup.ModSetup;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public class ShopTablet extends LoreItem{
    public static final String SCREEN_ADMINSHOP_SHOP = "screen.adminshop.shop";
    public ShopTablet() {
        super(new Item.Properties(), "A very sophisticated PDA. Right-click to open the Shop");
    }

    @Override
    public int getMaxStackSize(ItemStack stack) {
        return 1;
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(Level level, @NotNull Player player, @NotNull InteractionHand pUsedHand) {
        if (!level.isClientSide) {
            ServerLevel serverLevel = (ServerLevel) level;
            ServerPlayer serverPlayer = (ServerPlayer) player;
            MenuProvider containerProvider = new MenuProvider() {
                @Override
                public @NotNull Component getDisplayName() {
                    return Component.translatable(SCREEN_ADMINSHOP_SHOP);
                }

                @Override
                public AbstractContainerMenu createMenu(int windowId, @NotNull Inventory playerInventory, @NotNull Player playerEntity) {
                    return new ShopMenu(windowId, playerInventory, playerEntity);
                }
            };
            // Update usable accounts
            MoneyHelper.MoneyAccount account = MoneyHelper.get(serverLevel).getPlayerAccount(serverPlayer);
            Messages.sendToPlayer(new PacketSyncMoneyToClient(account), (ServerPlayer) player);
            ((ServerPlayer) player).openMenu(containerProvider);
        }
        return super.use(level, player, pUsedHand);
    }
}
