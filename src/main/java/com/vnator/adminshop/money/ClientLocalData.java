package com.vnator.adminshop.money;

import com.ibm.icu.impl.Pair;
import com.vnator.adminshop.AdminShop;
import com.vnator.adminshop.shop.ShopItem;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClientLocalData {
    // Only contains the list of accounts it is either an owner or member on
    private static final List<BankAccount> usableAccounts = new ArrayList<>();
    private static final Map<Pair<String, Integer>, BankAccount> accountMap = new HashMap<>();

    private static final Map<BlockPos, Pair<String, Integer>> machineAccountMap = new HashMap<>();
    private static final Map<BlockPos, String> machineOwnerMap = new HashMap<>();
    private static final Map<BlockPos, ShopItem> buyerTargetMap = new HashMap<>();

    public static void addBuyerTarget(BlockPos pos, ShopItem target) {
        System.out.println("Added target to client local data");
        buyerTargetMap.put(pos, target);
    }
    public static boolean hasTarget(BlockPos pos) {
        return buyerTargetMap.containsKey(pos);
    }
    public static ShopItem getBuyerTarget(BlockPos pos) {
        return buyerTargetMap.get(pos);
    }
    public static List<BankAccount> getUsableAccounts() {
        return usableAccounts;
    }

    public static void addMachineOwner(BlockPos pos, String owner) {
        machineOwnerMap.put(pos, owner);
    }
    public static String getMachineOwner(BlockPos pos) {
        return machineOwnerMap.get(pos);
    }
    public static void addMachineAccount(BlockPos pos, Pair<String, Integer> owner) {
        machineAccountMap.put(pos, owner);
    }
    public static Pair<String, Integer> getMachineAccount(BlockPos pos) {
        return machineAccountMap.get(pos);
    }

    public static Map<Pair<String, Integer>, BankAccount> getAccountMap() {
        return accountMap;
    }

    public static void setUsableAccounts(List<BankAccount> usableAccounts) {
        ClientLocalData.usableAccounts.clear();
        ClientLocalData.usableAccounts.addAll(usableAccounts);
        ClientLocalData.accountMap.clear();
        ClientLocalData.usableAccounts.forEach(account -> {
            ClientLocalData.accountMap.put(Pair.of(account.getOwner(), account.getId()), account);
        });
        sortUsableAccounts();
    }

    // Sort usableAccounts, first by pair.first == playerUUID, if not sort alphabetically, then by pair.second in
    // ascending order. Index is preserved to the original account it pointed to.
    public static void sortUsableAccounts() {
        ClientLocalData.usableAccounts.sort((o1, o2) -> {
            assert Minecraft.getInstance().player != null;
            String playerUUID = Minecraft.getInstance().player.getStringUUID();
            if (o1.getOwner().equals(playerUUID) && !o2.getOwner().equals(playerUUID)) {
                return -1;
            } else if (!o1.getOwner().equals(playerUUID) && o2.getOwner().equals(playerUUID)) {
                return 1;
            } else if (o1.getOwner().equals(o2.getOwner())) {
                return Integer.compare(o1.getId(), o2.getId());
            } else {
                return o1.getOwner().compareTo(o2.getOwner());
            }
        });
    }

    public static BankAccount addAccount(BankAccount newAccount) {
        if (accountMap.containsKey(Pair.of(newAccount.getOwner(), newAccount.getId()))) {
            AdminShop.LOGGER.warn("newAccount already in accountMap.");
            return accountMap.get(Pair.of(newAccount.getOwner(), newAccount.getId()));
        }
        ClientLocalData.usableAccounts.add(newAccount);
        ClientLocalData.accountMap.put(Pair.of(newAccount.getOwner(), newAccount.getId()), newAccount);
        return newAccount;
    }

    public static long getMoney(Pair<String, Integer> bankAccount) {
//        if (!accountSet.contains(bankAccount)) {
        BankAccount result;
        if (!accountMap.containsKey(bankAccount)) {
            AdminShop.LOGGER.warn("Could not find bankAccount in usableAccounts, adding it.");
            AdminShop.LOGGER.warn("OwnerUUID:" + bankAccount.first + ", accID:" + bankAccount.second);
            result = ClientLocalData.addAccount(new BankAccount(bankAccount.first, bankAccount.second));
        } else {
            result = accountMap.get(bankAccount);
        }
        assert bankAccount != null;
        return result.getBalance();
    }

}
