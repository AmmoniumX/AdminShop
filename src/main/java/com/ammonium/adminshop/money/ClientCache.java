package com.ammonium.adminshop.money;

import dev.ftb.mods.ftbteams.api.FTBTeamsAPI;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.UUID;

public class ClientCache {

    private static MoneyHelper.MoneyAccount account = null;

    public static @Nullable MoneyHelper.MoneyAccount getAccount() {
        return account;
    }

    public static boolean hasPermit(String permit) {
        if (permit == null || permit.isEmpty()) { return true; }
        if (account == null) { return false; }
        return account.permits().contains(permit);
    }

    public static void setAccount(UUID teamId, long balance, Collection<String> permits) {
//        AdminShop.LOGGER.debug("Set account: {} {} {}", teamId, balance, permits);
        Component name = FTBTeamsAPI.api().getClientManager().selfTeam().getName();
        account = new MoneyHelper.MoneyAccount(teamId, name, balance, permits);
//        AdminShop.LOGGER.debug("Set account: {}", account);
    }

}
