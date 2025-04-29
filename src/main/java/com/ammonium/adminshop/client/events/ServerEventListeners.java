package com.ammonium.adminshop.client.events;

import com.ammonium.adminshop.AdminShop;
import com.ammonium.adminshop.client.jei.PreparableReloadListener;
import com.ammonium.adminshop.commands.AdminShopCommand;
import com.ammonium.adminshop.money.MoneyHelper;
import com.ammonium.adminshop.network.PacketSyncMoneyToClient;
import com.ammonium.adminshop.setup.Messages;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = AdminShop.MODID)
public class ServerEventListeners {

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event){
        if (event.getEntity().level.isClientSide()) { return; }
        ServerLevel level = (ServerLevel) event.getEntity().level;
        ServerPlayer player = (ServerPlayer) event.getEntity();
        MoneyHelper.MoneyAccount account = MoneyHelper.get(level).getPlayerAccount(player);
        Messages.sendToPlayer(new PacketSyncMoneyToClient(account), player);
    }

    @SubscribeEvent
    public static void onCommandRegistration(RegisterCommandsEvent event){
        CommandDispatcher<CommandSourceStack> commandDispatcher = event.getDispatcher();
        AdminShopCommand.register(commandDispatcher);
    }

    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event) {
        // Do something when the server starts
//        AdminShop.LOGGER.info("Loading Shop from server start");
    }

    @SubscribeEvent
    public static void addReloadListener(AddReloadListenerEvent event) {
        event.addListener(new PreparableReloadListener());
    }

}
