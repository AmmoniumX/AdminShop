package com.ammonium.adminshop.client.events;

import com.ammonium.adminshop.AdminShop;
import com.ammonium.adminshop.commands.AdminShopCommand;
import com.ammonium.adminshop.money.MoneyHelper;
import com.ammonium.adminshop.network.PacketSyncMoneyToClient;
import com.ammonium.adminshop.recipes.ModRecipeSerializers;
import com.ammonium.adminshop.recipes.ModRecipeTypes;
import com.ammonium.adminshop.setup.Messages;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;

import java.nio.file.Files;
import java.nio.file.Path;

@EventBusSubscriber(modid = AdminShop.MODID, bus = EventBusSubscriber.Bus.GAME)
public class ServerEventListeners {
    private static final Path OLD_SHOP_PATH = FMLPaths.CONFIGDIR.get().resolve("adminshop/shop.csv");
    private static boolean oldShopPathExists = false;

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event){
        if (event.getEntity().level().isClientSide()) { return; }
        ServerLevel level = (ServerLevel) event.getEntity().level();
        AdminShop.LOGGER.info("AdminShop recipes: buy_items={}, sell_items={}, buy_fluids={}, sell_fluids={}",
            level.getRecipeManager().getAllRecipesFor(ModRecipeTypes.SHOP_BUY_ITEM.get()).size(),
            level.getRecipeManager().getAllRecipesFor(ModRecipeTypes.SHOP_SELL_ITEM.get()).size(),
            level.getRecipeManager().getAllRecipesFor(ModRecipeTypes.SHOP_BUY_FLUID.get()).size(),
            level.getRecipeManager().getAllRecipesFor(ModRecipeTypes.SHOP_SELL_FLUID.get()).size()
        );
        ServerPlayer player = (ServerPlayer) event.getEntity();
        MoneyHelper.MoneyAccount account = MoneyHelper.get(level).getPlayerAccount(player);
        Messages.sendToPlayer(new PacketSyncMoneyToClient(account), player);
        if (oldShopPathExists) {
            player.sendSystemMessage(Component.translatable(
                    "message.adminshop.shop_csv_found_1"
            ));
            player.sendSystemMessage(Component.translatable(
                    "message.adminshop.shop_csv_found_2"
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
        ModRecipeSerializers.logRegisteredSerializers();
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
