package com.ammonium.adminshop;

import com.ammonium.adminshop.blocks.ModBlocks;
import com.ammonium.adminshop.blocks.entity.ModBlockEntities;
import com.ammonium.adminshop.item.ModItems;
import com.ammonium.adminshop.recipes.ModRecipeSerializers;
import com.ammonium.adminshop.recipes.ModRecipeTypes;
import com.ammonium.adminshop.screen.ModMenuTypes;
import com.ammonium.adminshop.setup.ClientSetup;
import com.ammonium.adminshop.setup.Config;
import com.ammonium.adminshop.setup.CreativeTab;
import com.ammonium.adminshop.setup.Messages;
import com.ammonium.adminshop.setup.ModSetup;
import com.mojang.logging.LogUtils;
//import net.minecraftforge.eventbus.api.IEventBus;
//import net.minecraftforge.fml.DistExecutor;
//import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import org.slf4j.Logger;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(AdminShop.MODID)
public class AdminShop {
    // Directly reference a slf4j logger
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final String MODID = "adminshop";

    private final ModContainer modContainer;

    public AdminShop(IEventBus eventBus, ModContainer p_modContainer) {

        this.modContainer = p_modContainer;
//        IEventBus eventBus = ModLoadingContext.get().getModEventBus();
        Config.register(modContainer);

        eventBus.addListener(ModSetup::init);
//        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> eventBus.addListener(ClientSetup::init));
        if (FMLLoader.getDist() == Dist.CLIENT) {
            eventBus.addListener(ClientSetup::init);
        }
//        MinecraftForge.EVENT_BUS.register(ServerEventListeners.class);

        eventBus.addListener(Messages::register);

        ModItems.register(eventBus);
        ModBlocks.register(eventBus);
        ModBlockEntities.register(eventBus);
        ModMenuTypes.register(eventBus);
        ModRecipeTypes.register(eventBus);
        ModRecipeSerializers.register(eventBus);
        CreativeTab.register(eventBus);

    }

    private void setup(final FMLCommonSetupEvent event) {

    }

    // You can use EventBusSubscriber to automatically subscribe events on the contained class (this is subscribing to the FORGE
    // Event bus for receiving Forge Events)
    @EventBusSubscriber(modid = MODID, bus = EventBusSubscriber.Bus.MOD)
    public static class ModEvents {
        @SubscribeEvent
        public static void registerCapabilities(RegisterCapabilitiesEvent event) {
            event.registerBlockEntity(
                    Capabilities.ItemHandler.BLOCK,
                    ModBlockEntities.SELLER.get(),
                    (be, side) -> be.inventory
            );
            event.registerBlockEntity(
                    Capabilities.FluidHandler.BLOCK,
                    ModBlockEntities.FLUID_BUYER.get(),
                    (be, side) -> be.getTank()
            );
            event.registerBlockEntity(
                    Capabilities.FluidHandler.BLOCK,
                    ModBlockEntities.FLUID_SELLER.get(),
                    (be, side) -> be.getTank()
            );
        }
    }
}