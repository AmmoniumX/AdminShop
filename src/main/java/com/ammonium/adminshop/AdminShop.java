package com.ammonium.adminshop;

import com.ammonium.adminshop.blocks.ModBlocks;
import com.ammonium.adminshop.blocks.entity.ModBlockEntities;
import com.ammonium.adminshop.item.ModItems;
import com.ammonium.adminshop.screen.ModMenuTypes;
import com.ammonium.adminshop.setup.ClientSetup;
import com.ammonium.adminshop.setup.Config;
import com.ammonium.adminshop.setup.ModSetup;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.logging.LogUtils;
import net.minecraft.commands.arguments.item.ItemParser;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.world.item.Item;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

import java.util.Optional;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(AdminShop.MODID)
public class AdminShop {
    // Directly reference a slf4j logger
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final String MODID = "adminshop";

    public AdminShop() {

        IEventBus eventBus = FMLJavaModLoadingContext.get().getModEventBus();
        Config.register();

        eventBus.addListener(ModSetup::init);
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> eventBus.addListener(ClientSetup::init));
//        MinecraftForge.EVENT_BUS.register(ServerEventListeners.class);

        ModItems.register(eventBus);
        ModBlocks.register(eventBus);
        ModBlockEntities.register(eventBus);
        ModMenuTypes.register(eventBus);

    }

    private static Optional<ItemParser.ItemResult> parseItem(String pattern) throws IllegalStateException {

        // Check for empty or null pattern
        if (pattern == null || pattern.isEmpty()) {
            LOGGER.debug("Pattern is null or empty");
            return Optional.empty();
        }

        StringReader reader = new StringReader(pattern);

        // Get the item registry
        Registry<?> rawItemRegistry = Registry.REGISTRY.get(Registry.ITEM_REGISTRY.registry());
        if (rawItemRegistry == null) {
            throw new IllegalStateException("Item registry not found");
        }
        //noinspection unchecked // Cast to Registry<Item> is safe because we know the registry is for items
        Registry<Item> itemRegistry = (Registry<Item>) rawItemRegistry;

        HolderLookup<Item> itemLookup = new HolderLookup.RegistryLookup<>(itemRegistry);
        try {
            ItemParser.ItemResult result = ItemParser.parseForItem(itemLookup, reader);
            return Optional.of(result);

        } catch (CommandSyntaxException e) {
            LOGGER.debug("Failed to parse item: {}", pattern);
            return Optional.empty();
        }
    }

    private void setup(final FMLCommonSetupEvent event) {

    }

    // You can use EventBusSubscriber to automatically subscribe events on the contained class (this is subscribing to the FORGE
    // Event bus for receiving Forge Events)
    @Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class ForgeEvents {
    }
}