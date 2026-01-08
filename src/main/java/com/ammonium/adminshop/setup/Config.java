package com.ammonium.adminshop.setup;

import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;

public class Config {

    public static ModConfigSpec.LongValue STARTING_MONEY;
    public static ModConfigSpec.BooleanValue balanceDisplay;
    public static ModConfigSpec.BooleanValue displayFormat;
    public static ModConfigSpec.BooleanValue ignoreDecimalOffset;
    public static ModConfigSpec.IntValue balanceDelta;

    public static void register(ModContainer modContainer){
        ModConfigSpec.Builder serverConfig = new ModConfigSpec.Builder();
        registerServerConfigs(serverConfig);
        modContainer.registerConfig(ModConfig.Type.SERVER, serverConfig.build());
        ModConfigSpec.Builder clientConfig = new ModConfigSpec.Builder();
        registerClientConfigs(clientConfig);
        modContainer.registerConfig(ModConfig.Type.CLIENT, clientConfig.build());
    }

    private static void registerServerConfigs(ModConfigSpec.Builder config){
        config.comment("General configurations. Shop contents stored in \"adminshop.csv\"")
                .push("server_config");
        STARTING_MONEY = config
                .comment("Amount of money each player starts with. Must be a whole number.")
                .defineInRange("starting_money", 100, 0, Long.MAX_VALUE);
        config.pop();
    }

    private static void registerClientConfigs(ModConfigSpec.Builder config){
        config.comment("Client configurations. Options for changing display view")
                .push("display_config");

        balanceDisplay = config
                .comment("Displays your current balance and gained balance per second in the top left corner")
                .define("Balance Display", true);

        displayFormat = config
                .comment("If monetary values should be formatted as M/B/T/etc (Short) instead of Million/Billion/Trillion/etc (Full)")
                .define("Short mode ", true );

        ignoreDecimalOffset = config
                .comment("Ignore decimal offset specified in language file. This will show all money values as whole numbers.")
                .define("Ignore decimal offset", false);

        balanceDelta = config
                .comment("Number of *seconds* sampled for showing average balance/second change. Smaller values may display more erratic values.")
                .defineInRange("Balance Delta", 5, 1, Integer.MAX_VALUE);

        config.pop();
    }

}
