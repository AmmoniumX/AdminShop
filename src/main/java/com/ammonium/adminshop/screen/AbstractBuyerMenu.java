package com.ammonium.adminshop.screen;

import com.ammonium.adminshop.AdminShop;
import com.ammonium.adminshop.blocks.entity.AbstractBuyerEntity;
import com.ammonium.adminshop.screen.slot.ResultSlot;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;

public abstract class AbstractBuyerMenu extends AbstractContainerMenu {
    private final AbstractBuyerEntity blockEntity;
    private final Level level;
    private final int SLOT_COUNT;
    private final int SLOT_START_X;

    protected abstract Block getBlockType();

    protected AbstractBuyerMenu(MenuType<? extends AbstractBuyerMenu> menuType, int slotCount, int slotStartX, int windowId, Inventory inv, FriendlyByteBuf extraData) {
        this(menuType, slotCount, slotStartX, windowId, inv, inv.player.level.getBlockEntity(extraData.readBlockPos()));
    }

    protected AbstractBuyerMenu(MenuType<? extends AbstractBuyerMenu> menuType, int slotCount, int slotStartX, int windowId, Inventory inv, BlockEntity entity) {
        super(menuType, windowId);
        this.SLOT_COUNT = slotCount;
        this.SLOT_START_X = slotStartX;
        this.blockEntity = ((AbstractBuyerEntity) entity);
        this.level = inv.player.level;
        checkContainerSize(inv, SLOT_COUNT);

        addPlayerInventory(inv);
        addPlayerHotbar(inv);

        this.blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER).ifPresent(handler -> {
            for (int i = 0; i < SLOT_COUNT; i++) {
                this.addSlot(new ResultSlot(handler, i, SLOT_START_X + (i * 18), 42));
            }
        });

    }

    public AbstractBuyerEntity getBlockEntity() {
        return blockEntity;
    }
    // CREDIT GOES TO: diesieben07 | https://github.com/diesieben07/SevenCommons
    // must assign a slot number to each of the slots used by the GUI.
    // For this container, we can see both the tile inventory's slots as well as the player inventory slots and the hotbar.
    // Each time we add a Slot to the container, it automatically increases the slotIndex, which means
    //  0 - 8 = hotbar slots (which will map to the InventoryPlayer slot numbers 0 - 8)
    //  9 - 35 = player inventory slots (which map to the InventoryPlayer slot numbers 9 - 35)
    //  36 - 44 = TileInventory slots, which map to our TileEntity slot numbers 0 - 8)
    private static final int HOTBAR_SLOT_COUNT = 9;
    private static final int PLAYER_INVENTORY_ROW_COUNT = 3;
    private static final int PLAYER_INVENTORY_COLUMN_COUNT = 9;
    private static final int PLAYER_INVENTORY_SLOT_COUNT = PLAYER_INVENTORY_COLUMN_COUNT * PLAYER_INVENTORY_ROW_COUNT;
    private static final int VANILLA_SLOT_COUNT = HOTBAR_SLOT_COUNT + PLAYER_INVENTORY_SLOT_COUNT;
    private static final int VANILLA_FIRST_SLOT_INDEX = 0;
    private static final int TE_INVENTORY_FIRST_SLOT_INDEX = VANILLA_FIRST_SLOT_INDEX + VANILLA_SLOT_COUNT;

    protected int getTeInventoryFirstSlotIndex() {
        return TE_INVENTORY_FIRST_SLOT_INDEX;
    }
    protected int getTeInventorySlotCount() {
        return SLOT_COUNT;
    }

    protected AbstractBuyerMenu(MenuType<? extends AbstractBuyerMenu> menuType, int slotCount, int slotStartX, int id, Inventory playerInventory, Level pLevel, BlockPos pPos) {
        this(menuType, slotCount, slotStartX, id, playerInventory, pLevel.getBlockEntity(pPos));
    }



    @Override
    protected boolean moveItemStackTo(ItemStack pStack, int pStartIndex, int pEndIndex, boolean pReverseDirection) {
        if (pStartIndex >= TE_INVENTORY_FIRST_SLOT_INDEX && pEndIndex < TE_INVENTORY_FIRST_SLOT_INDEX
                + SLOT_COUNT) {
            AdminShop.LOGGER.debug("Cannot move item stack here");
            return false;
        }
        return super.moveItemStackTo(pStack, pStartIndex, pEndIndex, pReverseDirection);
    }

    @Override
    public ItemStack quickMoveStack(Player playerIn, int index) {
        Slot sourceSlot = slots.get(index);
        if (sourceSlot == null || !sourceSlot.hasItem()) return ItemStack.EMPTY;  //EMPTY_ITEM
        ItemStack sourceStack = sourceSlot.getItem();
        ItemStack copyOfSourceStack = sourceStack.copy();
//        System.out.println("quickMoveStack()");
        // Check if the slot clicked is one of the block container slots
        if (index >= TE_INVENTORY_FIRST_SLOT_INDEX && index < TE_INVENTORY_FIRST_SLOT_INDEX + SLOT_COUNT) {
//            System.out.println("Is in one of the block container slots");
            // This is a TE slot so merge the stack into the players inventory
            if (!moveItemStackTo(sourceStack, VANILLA_FIRST_SLOT_INDEX, VANILLA_FIRST_SLOT_INDEX + VANILLA_SLOT_COUNT, false)) {
                return ItemStack.EMPTY;
            }
        }
        else {
            System.out.println("Invalid slotIndex:" + index);
            return ItemStack.EMPTY;  // EMPTY_ITEM
        }
        // If stack size == 0 (the entire stack was moved) set slot contents to null
        if (sourceStack.getCount() == 0) {
            sourceSlot.set(ItemStack.EMPTY);
        } else {
            sourceSlot.setChanged();
        }
        sourceSlot.onTake(playerIn, sourceStack);
        return copyOfSourceStack;
    }

    @Override
    public boolean stillValid(Player pPlayer) {
        return stillValid(ContainerLevelAccess.create(level, blockEntity.getBlockPos()),
                pPlayer, getBlockType());
    }

    private void addPlayerInventory(Inventory playerInventory) {
        for (int i = 0; i < 3; ++i) {
            for (int l = 0; l < 9; ++l) {
                this.addSlot(new Slot(playerInventory, l + i * 9 + 9, 8 + l * 18, 84 + i * 18));
            }
        }
    }

    private void addPlayerHotbar(Inventory playerInventory) {
        for (int i = 0; i < 9; ++i) {
            this.addSlot(new Slot(playerInventory, i, 8 + i * 18, 142));
        }
    }
}
