package com.ammonium.adminshop.screen;

import com.ammonium.adminshop.blocks.entity.BasicDetectorBE;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;
import net.minecraftforge.items.wrapper.InvWrapper;
import org.jetbrains.annotations.NotNull;

public class DetectorMenu extends AbstractContainerMenu {

    private final Player playerEntity;
    private final IItemHandler playerInventory;
    private final BasicDetectorBE basicDetectorBE;
    private final Level level;

    public DetectorMenu(int windowId, Inventory inv, FriendlyByteBuf extraData){
        this(windowId, inv, inv.player.level.getBlockEntity(extraData.readBlockPos()));
    }
    public DetectorMenu(int id, Inventory playerInventory, Level pLevel, BlockPos pPos) {
        this(id, playerInventory, pLevel.getBlockEntity(pPos));
    }

    public DetectorMenu(int windowId, Inventory inv, BlockEntity entity) {
        super(ModMenuTypes.DETECTOR_MENU.get(), windowId);
//        AdminShop.LOGGER.debug("Creating DetectorMenu");
        this.basicDetectorBE = ((BasicDetectorBE) entity);
        this.playerEntity = inv.player;
        this.level = inv.player.level;

        this.playerInventory = new InvWrapper(inv);
        layoutPlayerInventorySlots(10, 70);
    }

    public BasicDetectorBE getBlockEntity() {
        return basicDetectorBE;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public @NotNull ItemStack quickMoveStack(Player pPlayer, int pIndex) {
        // Since there's no other inventory to move items to, just return an empty ItemStack
        return ItemStack.EMPTY;
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