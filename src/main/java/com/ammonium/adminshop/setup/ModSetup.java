package com.ammonium.adminshop.setup;

import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

public class ModSetup {

    public static void init(FMLCommonSetupEvent event){
        Messages.register();

    }
}
