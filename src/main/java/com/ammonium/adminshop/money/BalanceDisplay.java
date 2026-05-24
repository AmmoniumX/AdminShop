package com.ammonium.adminshop.money;

import com.ammonium.adminshop.AdminShop;
import com.ammonium.adminshop.setup.Config;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(modid = AdminShop.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public class BalanceDisplay {
    private static long balance = 0;
    private static final long[] history = new long[]{0, 0};
    private static int tick = 0;
    private static int displayTick = 0;
    private static String displayString = "";
    private static boolean BALANCE_DISPLAY = false;
    private static int BALANCE_DELTA_SECONDS = -1;
    private static int BALANCE_DELTA_TICKS = -1;

    private static void loadConfig() {
        BALANCE_DISPLAY = Config.balanceDisplay.get();
        BALANCE_DELTA_SECONDS = Config.balanceDelta.get();
        BALANCE_DELTA_TICKS = BALANCE_DELTA_SECONDS * 20;
    }

    private static boolean shouldRun() {
        return BALANCE_DISPLAY && (BALANCE_DELTA_SECONDS > 0);
    }

    private static void reset() {
        history[0] = history[1] = 0;
        tick = 0;
        displayTick = 0;
        displayString = "";
    }

    private static void updateDisplayString() {
        long changePerSecond = (history[1] - history[0]) / BALANCE_DELTA_SECONDS;
        StringBuilder str = new StringBuilder(MoneyFormat.cfgformat(balance));

        if (changePerSecond != 0) {
            str.append(" ")
                    .append(changePerSecond > 0 ? ChatFormatting.GREEN + "+" : ChatFormatting.RED)
                    .append(MoneyFormat.format(changePerSecond, MoneyFormat.FormatType.SHORT, MoneyFormat.FormatType.RAW))
                    .append("/s");
        }

        displayString = Component.translatable("gui.balance", str.toString()).getString();
    }

    @EventBusSubscriber(modid = AdminShop.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ModEvents {
        @SubscribeEvent
        public static void onConfigLoad(ModConfigEvent event) {
            if (event.getConfig().getModId().equals(AdminShop.MODID)) {
                loadConfig();
            }
        }
    }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        if (!shouldRun()) return;
        if (Minecraft.getInstance().player == null) return;

        // Update display string every second (20 ticks)
        if (displayTick <= 0) {
            displayTick = 20;
            updateDisplayString();
        }
        displayTick--;

        // Update balance history at configured interval
        if (tick <= 0) {
            tick = BALANCE_DELTA_TICKS;
            MoneyHelper.MoneyAccount account = ClientCache.getAccount();
            if (account == null) {
                return;
            }
            balance = account.balance();
            history[0] = history[1];
            history[1] = balance;
        }
        tick--;
    }

    @SubscribeEvent
    public static void clientDisconnect(ClientPlayerNetworkEvent.LoggingOut event) {
        reset();
    }

    @SubscribeEvent
    public static void onRenderGUI(RenderGuiEvent.Post event) {
        if (!shouldRun()) return;
        if (displayString.isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        event.getGuiGraphics().drawString(mc.font, displayString, 2, 2, 0xFFFFFF);
    }
}