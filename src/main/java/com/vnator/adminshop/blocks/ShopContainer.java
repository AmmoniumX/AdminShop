package com.vnator.adminshop.blocks;

import com.ibm.icu.impl.Pair;
import com.vnator.adminshop.AdminShop;
import com.vnator.adminshop.money.BankAccount;
import com.vnator.adminshop.money.ClientMoneyData;
import com.vnator.adminshop.money.MoneyManager;
import com.vnator.adminshop.setup.Registration;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;
import net.minecraftforge.items.wrapper.InvWrapper;

import java.util.Optional;
import java.util.Set;

public class ShopContainer extends AbstractContainerMenu {

    private Player playerEntity;
    private IItemHandler playerInventory;

    public ShopContainer(int windowId, Inventory playerInventory, Player player){
        super(Registration.SHOP_CONTAINER.get(), windowId);
        this.playerEntity = player;
        this.playerInventory = new InvWrapper(playerInventory);

        layoutPlayerInventorySlots(16, 140);
//        trackMoney();
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    private void trackMoney(){
        addDataSlot(new DataSlot() {
            @Override public int get() {return (int)(getPlayerBalance() & 0xFFFF);}
            @Override public void set(int value) {setPlayerBalance(value, 0);}
        });
        addDataSlot(new DataSlot() {
            @Override public int get() {return (int)((getPlayerBalance() >> 16) & 0xFFFF);}
            @Override public void set(int value) {setPlayerBalance(value, 1);}
        });
        addDataSlot(new DataSlot() {
            @Override public int get() {return (int)((getPlayerBalance() >> 32) & 0xFFFF);}
            @Override public void set(int value) {setPlayerBalance(value, 2);}
        });
        addDataSlot(new DataSlot() {
            @Override public int get() {return (int)((getPlayerBalance() >> 48) & 0xFFFF);}
            @Override public void set(int value) {setPlayerBalance(value, 3);}
        });
    }

    public long getPlayerBalance(){
        // add BankAccount-s functionality
        if(playerEntity.level.isClientSide()){
            Set<BankAccount> accountSet = ClientMoneyData.getAccountSet();
            BankAccount personalAccount;
            Optional<BankAccount> result = accountSet.stream().filter(account -> (account.getOwner().equals(playerEntity.getStringUUID()) &&
                    account.getId() == 1)).findAny();
            if (result.isEmpty()) {
                AdminShop.LOGGER.warn("(Container) Couldn't find personal account, creating one.");
                personalAccount = new BankAccount(playerEntity.getStringUUID());
                accountSet.add(personalAccount);
                ClientMoneyData.setAccountSet(accountSet);
            } else {
                personalAccount = result.get();
            }
            return personalAccount.getBalance();
        }
        return MoneyManager.get(playerEntity.level).getBalance(playerEntity.getStringUUID(), 1);
    }

    public void setPlayerBalance(int value, int index){
        // add BankAccount-s functionality
        long money = getPlayerBalance();
        long change = (long) value << (index*16);

        if(playerEntity.level.isClientSide()){
//            ClientMoneyData.setMoney(playerEntity.getStringUUID(), 1, money | change);
            Set<BankAccount> accountSet = ClientMoneyData.getAccountSet();
            BankAccount personalAccount;
            Optional<BankAccount> result = accountSet.stream().filter(account -> (account.getOwner().equals(playerEntity.getStringUUID()) &&
                    account.getId() == 1)).findAny();
            if (result.isEmpty()) {
                AdminShop.LOGGER.warn("(Container) Couldn't find personal account, creating one.");
                personalAccount = new BankAccount(playerEntity.getStringUUID());
                accountSet.add(personalAccount);
                ClientMoneyData.setAccountSet(accountSet);
            } else {
                personalAccount = result.get();
            }
            personalAccount.setBalance(money | change);
        } else {
            AdminShop.LOGGER.error("Calling setPlayerBalance() out of client side!");
        }
    }

    private int addSlotRange(IItemHandler handler, int index, int x, int y, int amount, int dx) {
        for (int i = 0 ; i < amount ; i++) {
            addSlot(new SlotItemHandler(handler, index, x, y));
            x += dx;
            index++;
        }
        return index;
    }

    private int addSlotBox(IItemHandler handler, int index, int x, int y, int horAmount, int dx, int verAmount, int dy) {
        for (int j = 0 ; j < verAmount ; j++) {
            index = addSlotRange(handler, index, x, y, horAmount, dx);
            y += dy;
        }
        return index;
    }

    private void layoutPlayerInventorySlots(int leftCol, int topRow) {
        // Player inventory
        addSlotBox(playerInventory, 9, leftCol, topRow, 9, 18, 3, 18);

        // Hotbar
        topRow += 58;
        addSlotRange(playerInventory, 0, leftCol, topRow, 9, 18);
    }
}
