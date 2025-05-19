package com.ammonium.adminshop.client.events;

import com.ammonium.adminshop.AdminShop;
import com.ammonium.adminshop.commands.AdminShopCommand;
import com.ammonium.adminshop.money.MoneyHelper;
import com.ammonium.adminshop.network.PacketSyncMoneyToClient;
import com.ammonium.adminshop.setup.Messages;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;

import java.nio.file.Files;
import java.nio.file.Path;

@Mod.EventBusSubscriber(modid = AdminShop.MODID)
public class ServerEventListeners {
    private static final Path OLD_SHOP_PATH = FMLPaths.CONFIGDIR.get().resolve("adminshop/shop.csv");
    private static boolean oldShopPathExists = false;

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event){
        if (event.getEntity().level.isClientSide()) { return; }
        ServerLevel level = (ServerLevel) event.getEntity().level;
        ServerPlayer player = (ServerPlayer) event.getEntity();
        MoneyHelper.MoneyAccount account = MoneyHelper.get(level).getPlayerAccount(player);
        Messages.sendToPlayer(new PacketSyncMoneyToClient(account), player);
        if (oldShopPathExists) {
            player.sendSystemMessage(Component.literal(
            "Shop.csv found in config folder. This is no longer used and will not be read from, use datapack recipes!"
            ));
            player.sendSystemMessage(Component.literal(
                    "Please delete the file at config/adminshop/shop.csv to disable this error."
            ));
        }
    }

    @SubscribeEvent
    public static void onCommandRegistration(RegisterCommandsEvent event){
        CommandDispatcher<CommandSourceStack> commandDispatcher = event.getDispatcher();
        AdminShopCommand.register(commandDispatcher);
    }

    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event) {
        if (Files.exists(OLD_SHOP_PATH)) {
            AdminShop.LOGGER.error("Shop.csv found in config folder. This is no longer used and will not be read from, use datapack recipes!");
            AdminShop.LOGGER.error("Please delete the file at config/adminshop/shop.csv to disable this error.");
            oldShopPathExists = true;
        }

    }

    @SubscribeEvent
    public static void addReloadListener(AddReloadListenerEvent event) {
        if (Files.exists(OLD_SHOP_PATH)) {
            AdminShop.LOGGER.error("Shop.csv found in config folder. This is no longer used and will not be read from, use datapack recipes!");
            AdminShop.LOGGER.error("Please delete the file at config/adminshop/shop.csv to disable this error.");
            oldShopPathExists = true;
        }
    }

}
