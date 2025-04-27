package com.ammonium.adminshop.client.jei;

import com.ammonium.adminshop.AdminShop;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;

public class PreparableReloadListener extends SimplePreparableReloadListener<String> {
    private static final Path CONFIG_PATH = FMLPaths.CONFIGDIR.get().resolve("adminshop/shop.csv");
    @Override
    protected String prepare(ResourceManager pResourceManager, ProfilerFiller pProfiler) {
        // Ensure the NIO approach is used only on the server side or in a common environment
        if (!Files.exists(CONFIG_PATH)) {
            // Return empty string if config not found
            AdminShop.LOGGER.error("Shop.csv not found!");
            return "";
        }

        // Use try-with-resources to ensure the reader is closed after use
        try (BufferedReader reader = Files.newBufferedReader(CONFIG_PATH)) {
            // Read the file into a String
            // This assumes the entire file content is to be read into a single String
            // If the file is large, consider processing it line by line instead
            return reader.lines().collect(Collectors.joining(System.lineSeparator()));
        } catch (IOException e) {
            // Handle the exception as appropriate
            AdminShop.LOGGER.error("IOException reading shop.csv");
            e.printStackTrace();
        }
        // Return empty string if IOException
        return "";
    }

    @Override
    protected void apply(String shopTextRaw, ResourceManager pResourceManager, ProfilerFiller pProfiler) {
//        if (!ServerEventListeners.getStartupCompleted()) {
//            AdminShop.LOGGER.debug("Startup hasn't completed, cancelling adminshop reload");
//            return;
//        }
//        AdminShop.LOGGER.debug("Reloading shop...");
//        AdminShop.LOGGER.debug("shopTextRaw length: "+shopTextRaw.length());
//        // Ensure we are on the server side
//        if (FMLEnvironment.dist == Dist.DEDICATED_SERVER || FMLEnvironment.dist == Dist.CLIENT) {
//            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
//            Shop.get().loadFromFile(shopTextRaw);
//            if (server != null) {
//                AdminShop.LOGGER.debug("Sending PacketSyncShopToClient to players from reload event listener...");
//                // Iterate over all online players and send the packet
//                for (ServerPlayer player : server.getPlayerList().getPlayers()) {
//                    Messages.sendToPlayer(new PacketSyncShopToClient(shopTextRaw), player);
//                }
//            }
//        }
    }
}
