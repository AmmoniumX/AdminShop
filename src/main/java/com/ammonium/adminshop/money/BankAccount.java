package com.ammonium.adminshop.money;

import com.ammonium.adminshop.AdminShop;
import com.ammonium.adminshop.setup.Config;
import net.minecraft.nbt.CompoundTag;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class BankAccount {
    public static final int MAX_MEMBERS = 8;
    private String owner;
    private Set<String> members;
    private Set<Integer> permits;
    private int id;
    private long balance;

    public BankAccount() { }

    // Create personal account (id: 1)
    public BankAccount(String own) {
        this.owner = own;
        this.members = new HashSet<>();
        this.members.add(own);
        this.id = 1;
        HashSet<Integer> unlocks = new HashSet<>();
        unlocks.add(0);
        this.permits = unlocks;
        this.balance = Config.STARTING_MONEY.get();
    }
    public BankAccount(String own, int nid) {
        this.owner = own;
        this.members = new HashSet<>();
        this.members.add(own);
        this.id = nid;
        HashSet<Integer> unlocks = new HashSet<>();
        unlocks.add(0);
        this.permits = unlocks;
        this.balance = (id == 1) ? Config.STARTING_MONEY.get() : 0L;
    }

    public BankAccount(String own, int nid, Set<String> mem) {
        this.owner = own;
        this.members = mem;
        this.id = nid;
        HashSet<Integer> unlocks = new HashSet<>();
        unlocks.add(0);
        this.permits = unlocks;
        this.balance = (nid == 1) ? Config.STARTING_MONEY.get() : 0L;
    }

    public BankAccount(String own, Set<String> mem, int nid, long bal) {
        this.owner = own;
        this.members = mem;
        this.id = nid;
        HashSet<Integer> unlocks = new HashSet<>();
        unlocks.add(0);
        this.permits = unlocks;
        this.balance = bal;
    }

    public BankAccount(String own, Set<String> mem, int nid, long bal, Set<Integer> permits) {
        this.owner = own;
        this.members = mem;
        this.id = nid;
        this.permits = permits;
        this.balance = bal;
    }

    public CompoundTag serializeTag() {
        CompoundTag tag = new CompoundTag();
        tag.putString("owner", owner);
        tag.putString("members", String.join(";", members));
        tag.putString("permits", permits.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(";")));
        tag.putInt("id", id);
        tag.putLong("balance", balance);
        return tag;
    }

    public static BankAccount deserializeTag(CompoundTag tag) {
        BankAccount bankAccount = new BankAccount();
        bankAccount.setOwner(tag.getString("owner"));
        String joinedMembers = tag.getString("members");
        Set<String> origMembers = new HashSet<>(Arrays.asList(joinedMembers.split(";")));
        bankAccount.setMembers(origMembers);
        String joinedPermits = tag.getString("permits");
        Set<Integer> origPermits = Arrays.stream(joinedPermits.split(";"))
                .map(Integer::parseInt)
                .collect(Collectors.toSet());
        bankAccount.setPermits(origPermits);
        bankAccount.setId(tag.getInt("id"));
        bankAccount.setBalance(tag.getLong("balance"));
        return bankAccount;
    }

    public long getBalance() {
        return balance;
    }
    public void setBalance(long balance) {
        this.balance = balance;
    }
    public int getId() {
        return id;
    }
    public void setId(int id) {
        this.id = id;
    }
    public String getOwner() {
        return owner;
    }
    private void setOwner(String owner) {
        this.owner = owner;
    }
    public Set<String> getMembers() {
        return members;
    }

    public boolean containsMember(String playerUUID) {
        return members.contains(playerUUID);
    }
    public Set<Integer> getPermits() {
        return permits;
    }

    public void setMembers(Set<String> members) {
        this.members = members;
    }
    public void setPermits(Set<Integer> permits) {
        this.permits = permits;
    }
    public void addPermit(int permit) {
        this.permits.add(permit);
    }
    public void removePermit(int permit) {
        this.permits.remove(permit);
    }
    public boolean hasPermit(int permit) {
        if (permit == 0) { return true; }
        return this.permits.contains(permit);
    }
    public boolean hasPermit(String permit) {
        if (permit.equals("0") || permit.isEmpty()) { return true; }
        return this.permits.contains(Integer.parseInt(permit));
    }
    public boolean addMember(String newMember) {
        if (id == 1) {
            AdminShop.LOGGER.error("Can't add members to personal account!");
            return false;
        }
        if (members.contains(newMember)) {
            AdminShop.LOGGER.error("Bank account already contains member!");
            return false;
        }
        if (members.size() >= MAX_MEMBERS) {
            AdminShop.LOGGER.error("Bank account at max member size!");
            return false;
        }
        members.add(newMember);
        AdminShop.LOGGER.info("Added "+newMember+" to bank account.");
        return true;
    }

    public boolean removeMember(String toRemove) {
        if (Objects.equals(toRemove, owner)) {
            AdminShop.LOGGER.error("Can't remove owner from his bank account");
            return false;
        }
        if (!members.contains(toRemove)) {
            AdminShop.LOGGER.error("Player is not a member of this bank account!");
            return false;
        }
        members.remove(toRemove);
        AdminShop.LOGGER.info("Removed "+toRemove+" from bank account.");
        return true;
    }

    public boolean addBalance(long add) {
        // Only accept positive values
        if (add <= 0) return false;
        // Overflow protection
        if (this.balance > Long.MAX_VALUE - add) {
            this.balance = Long.MAX_VALUE; // Set to max value if overflow would occur
        } else {
            this.balance += add; // Normal operation
        }
        return true;
    }

    public boolean subtractBalance(long sub) {
        // Only accept positive values
        if (sub <= 0) return false;
        // Fail if insufficient balance
        if (this.balance < sub) {
            return false;
        }
        this.balance -= sub;
        return true;
    }
}
