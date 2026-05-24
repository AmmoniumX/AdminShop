package com.ammonium.adminshop.money;

import com.ammonium.adminshop.AdminShop;
import com.ammonium.adminshop.network.PacketSyncMoneyToClient;
import com.ammonium.adminshop.setup.Config;
import com.ammonium.adminshop.setup.Messages;
import com.google.common.collect.ImmutableSet;
import dev.ftb.mods.ftbteams.api.FTBTeamsAPI;
import dev.ftb.mods.ftbteams.api.Team;
import dev.ftb.mods.ftbteams.api.TeamManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class MoneyHelper extends SavedData {

    public static final String DATA_NAME = "adminshop:accounts";
    private ListTag ledger = new ListTag();

    public MoneyHelper() {}

    public MoneyHelper(CompoundTag tag) {
        super();
        ledger = tag.getList(DATA_NAME, ListTag.TAG_COMPOUND);
        AdminShop.LOGGER.debug("Loaded data tag: {}", ledger);
    }

    //"Singleton" getter
    public static MoneyHelper get(Level checkLevel){
        if(checkLevel.isClientSide()){
            throw new RuntimeException("Don't access this client-side!");
        }
        MinecraftServer serv = ServerLifecycleHooks.getCurrentServer();
        ServerLevel level = serv.getLevel(Level.OVERWORLD);
        assert level != null;
        DimensionDataStorage storage = level.getDataStorage();
        return storage.computeIfAbsent(new SavedData.Factory<>(MoneyHelper::new, (tag, provider) -> new MoneyHelper(tag), null), "adminshop:accounts");
    }

    @Override
    public @NotNull CompoundTag save(@NotNull CompoundTag compoundTag, net.minecraft.core.HolderLookup.Provider provider) {
        AdminShop.LOGGER.debug("Saving ledger: {}", ledger);
        if (ledger != null) {
            compoundTag.put(DATA_NAME, ledger);
        }
        return compoundTag;
    }

    public record MoneyAccount(UUID teamId, Component name, long balance, ImmutableSet<String> permits) {
        public MoneyAccount(UUID teamId, Component name, long balance, Collection<String> permits) {
            this(teamId, name, balance, ImmutableSet.copyOf(permits));
        }
    }

    public static boolean hasPermit(MoneyAccount account, String permit) {
        if (permit == null || permit.isEmpty()) { return true; }
        return account.permits.contains(permit);
    }

    private MoneyAccount getFromTag(Component name, CompoundTag tag) {
        UUID teamId = tag.getUUID("team");
        long balance = tag.getLong("balance");
        ListTag permitsList = tag.getList("permits", ListTag.TAG_STRING);
        Set<String> permits = new HashSet<>();
        for (int i = 0; i < permitsList.size(); i++) {
            permits.add(permitsList.getString(i));
        }
        return new MoneyAccount(teamId, name, balance, ImmutableSet.copyOf(permits));
    }

    private CompoundTag toTag(MoneyAccount account) {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("team", account.teamId);
        tag.putLong("balance", account.balance);
        ListTag permitsList = new ListTag();
        for (String permit : account.permits) {
            permitsList.add(StringTag.valueOf(permit));
        }
        tag.put("permits", permitsList);
        return tag;
    }

    private Optional<CompoundTag> getAccountTag(UUID teamId) {
        for (int i = 0; i < ledger.size(); i++) {
            CompoundTag accountTag = ledger.getCompound(i);
//            AdminShop.LOGGER.debug("Account tag: {}", accountTag);
            if (accountTag.getUUID("team").equals(teamId)) {
//                AdminShop.LOGGER.debug("Found account tag: {}", accountTag);
                return Optional.of(accountTag);
            }
        }
        AdminShop.LOGGER.debug("Account tag not found for teamId: {}", teamId);
        return Optional.empty();
    }

    private void updateAccount(MoneyAccount account, TeamManager manager) {
//        AdminShop.LOGGER.debug("updateAccount: {}", account.teamId);
        CompoundTag tag;
        Optional<CompoundTag> existingTag = getAccountTag(account.teamId);

        if (existingTag.isPresent()) {
            // Update the existing account
//            AdminShop.LOGGER.debug("Account found, so updating it");
            tag = existingTag.get();
        } else {
            // Create a new account
//            AdminShop.LOGGER.debug("Account not found, so creating a new one");
            tag = new CompoundTag();
            tag.putUUID("team", account.teamId);
            ledger.add(tag);
        }

        // Update the tag
        tag.putLong("balance", account.balance);
        ListTag permitsList = new ListTag();
        for (String permit : account.permits) {
            permitsList.add(StringTag.valueOf(permit));
        }
//        AdminShop.LOGGER.debug("Saving tag: {}, {}, {}", account.teamId, tag, ledger);
        tag.put("permits", permitsList);
        this.setDirty();

        // Sync the data to all players
        Optional<Team> oTeam = getFTBTeam(account.teamId, manager);
        if (oTeam.isEmpty()) {
            AdminShop.LOGGER.warn("Team not found for UUID: {}", account.teamId);
            return;
        }
        Team team = oTeam.get();
        for (ServerPlayer player : team.getOnlineMembers()) {
//            AdminShop.LOGGER.debug("Syncing account to player: {}", player.getName().getString());
            Messages.sendToPlayer(new PacketSyncMoneyToClient(account), player);
        }
    }

    public @NotNull MoneyAccount getPlayerAccount(@NotNull ServerPlayer player) {
            TeamManager manager = FTBTeamsAPI.api().getManager();
            Optional<Team> oTeam = manager.getTeamForPlayer(player);

            if (oTeam.isEmpty()) {
                AdminShop.LOGGER.debug("Player {} is not in a team", player.getName().getString());
                MoneyAccount account = new MoneyAccount(
                        player.getUUID(), player.getName(), Config.STARTING_MONEY.get(), ImmutableSet.of());
                updateAccount(account, manager);
                return account;
            }

            Team team = oTeam.get();
            Optional<CompoundTag> accountTag = getAccountTag(team.getId());

            if (accountTag.isEmpty()) {
                AdminShop.LOGGER.debug("Creating new account for team: {}", team.getName().getString());
                MoneyAccount account = new MoneyAccount(
                        team.getId(), team.getName(), Config.STARTING_MONEY.get(), ImmutableSet.of());
                updateAccount(account, manager);
                return account;
            }

            return getFromTag(team.getName(), accountTag.get());
        }

    public @Nullable MoneyAccount getAccountById(UUID teamId) {
        TeamManager manager = FTBTeamsAPI.api().getManager();
        Optional<Team> oTeam = getFTBTeam(teamId, manager);
        if (oTeam.isEmpty()) {
            AdminShop.LOGGER.debug("Team not found for UUID: {}", teamId);
            return null;
        }
        Team team = oTeam.get();
        // Check if there is an account for the team
        Optional<CompoundTag> accountTag = getAccountTag(teamId);
        return accountTag.map(tag -> getFromTag(team.getName(), tag)).orElse(null);
    }

    private static Optional<Team> getFTBTeam(UUID teamId, TeamManager manager) {
        if (teamId == null || manager == null) {
            AdminShop.LOGGER.debug("Team ID or manager is null");
            return Optional.empty();
        }
        return manager.getTeamByID(teamId);
    }

    public void setMoney(UUID teamId, long amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Amount cannot be negative");
        }
        TeamManager manager = FTBTeamsAPI.api().getManager();
        Optional<Team> oTeam = getFTBTeam(teamId, manager);
        if (oTeam.isEmpty()) {
            AdminShop.LOGGER.debug("Team not found for UUID: {}", teamId);
            return;
        }
        Team team = oTeam.get();
        Component teamName = team.getName();

        MoneyAccount account = new MoneyAccount(team.getId(), teamName, amount, ImmutableSet.of());
        updateAccount(account, manager);
        return;
    }

    public void addMoney(UUID teamId, long amount) {
        AdminShop.LOGGER.debug("Adding money: {} {}", teamId, amount);
        if (amount < 0) {
            throw new IllegalArgumentException("Amount cannot be negative");
        }
        TeamManager manager = FTBTeamsAPI.api().getManager();
        Optional<Team> oTeam = getFTBTeam(teamId, manager);
        MoneyAccount account = getAccountById(teamId);
        if (oTeam.isEmpty() || account == null) {
            AdminShop.LOGGER.debug("Team not found for UUID: {}", teamId);
            return;
        }
        Team team = oTeam.get();
        Component teamName = team.getName();

        long newBalance = account.balance + amount;
        MoneyAccount newAccount = new MoneyAccount(team.getId(), teamName, newBalance, account.permits);
        updateAccount(newAccount, manager);
        return;
    }

    public boolean removeMoney(UUID teamId, long amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Amount cannot be negative");
        }
        TeamManager manager = FTBTeamsAPI.api().getManager();
        Optional<Team> oTeam = getFTBTeam(teamId, manager);
        MoneyAccount account = getAccountById(teamId);
        if (oTeam.isEmpty() || account == null) {
            AdminShop.LOGGER.debug("Team not found for UUID: {}", teamId);
            return false;
        }
        Team team = oTeam.get();
        Component teamName = team.getName();

        if (account.balance < amount) {
            return false;
        }
        long newBalance = account.balance - amount;
        MoneyAccount newAccount = new MoneyAccount(team.getId(), teamName, newBalance, account.permits);
        updateAccount(newAccount, manager);
        return true;
    }

    public boolean hasPermit(UUID teamId, String permit) {
        if (permit == null || permit.isEmpty()) {
            return true;
        }
        MoneyAccount account = getAccountById(teamId);
        if (account == null) {
            AdminShop.LOGGER.debug("Team not found for UUID: {}", teamId);
            return false;
        }
        return account.permits.contains(permit);
    }

    public void addPermit(UUID teamId, String permit) {
        if (permit.isEmpty()) {
            throw new IllegalArgumentException("Permit cannot be empty");
        }
        TeamManager manager = FTBTeamsAPI.api().getManager();
        Optional<Team> oTeam = getFTBTeam(teamId, manager);
        MoneyAccount account = getAccountById(teamId);
        if (oTeam.isEmpty() || account == null) {
            AdminShop.LOGGER.debug("Team not found for UUID: {}", teamId);
            return;
        }
        Team team = oTeam.get();
        Set<String> newPermits = new HashSet<>(account.permits);
        newPermits.add(permit);
        MoneyAccount newAccount = new MoneyAccount(team.getId(), team.getName(), account.balance, newPermits);
        updateAccount(newAccount, manager);
        return;
    }

    public void removePermit(UUID teamId, String permit) {
        if (permit.isEmpty()) {
            throw new IllegalArgumentException("Permit cannot be empty");
        }
        TeamManager manager = FTBTeamsAPI.api().getManager();
        Optional<Team> oTeam = getFTBTeam(teamId, manager);
        MoneyAccount account = getAccountById(teamId);
        if (oTeam.isEmpty() || account == null) {
            AdminShop.LOGGER.debug("Team not found for UUID: {}", teamId);
            return;
        }
        Team team = oTeam.get();
        Set<String> newPermits = new HashSet<>(account.permits);
        newPermits.remove(permit);
        MoneyAccount newAccount = new MoneyAccount(team.getId(), team.getName(), account.balance, newPermits);
        updateAccount(newAccount, manager);
        return;
    }

    public boolean isMemberOfTeam(UUID teamId, ServerPlayer player) {
        assert teamId != null && player != null;
        TeamManager manager = FTBTeamsAPI.api().getManager();
        Optional<Team> oTeam = getFTBTeam(teamId, manager);
        if (oTeam.isEmpty()) {
            AdminShop.LOGGER.debug("Team not found for UUID: {}", teamId);
            return false;
        }
        Team team = oTeam.get();
        return team.getMembers().contains(player.getUUID());
    }
}
