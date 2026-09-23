package gd.rf.kongzhongtitian.PipeAPI;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;

public class TransporterNodeMenu extends AbstractContainerMenu {
    private final ContainerLevelAccess access;
    private static TransporterNodeBlockEntity blockEntity;

    // ===== 槽位坐标（供 Menu 与 Screen 共用，避免不一致）=====
    public static final int CACHE_X = 80,  CACHE_Y = 20;
    public static final int FILTER_X = 44, FILTER_Y = 50;
    public static final int SPEED_X = 80,  SPEED_Y = 50;
    public static final int RESERVED_X = 116, RESERVED_Y = 50;
    //public static final int DIR_Y = 96;
    //public static final int[] DIR_X = {26, 44, 62, 80, 98, 116};
    public static final int[] DIR_Y = {20, 38, 20, 2, 20, 38};
	public static final int[] DIR_X = {152, 134, 116, 134, 134, 152};

    // 玩家背包起始 Y
    public static final int INV_Y = 128;
    public static final int HOTBAR_Y = 186;

    public TransporterNodeMenu(int windowId, Inventory playerInv,
                               TransporterNodeBlockEntity blockEntity,
                               IItemHandler nodeInventory,
                               ContainerLevelAccess access) {
        super(DTMenu.TRANSPORTER_NODE_MENU.get(), windowId);
        this.blockEntity = blockEntity;
        this.access = access;

        // 缓存槽
        this.addSlot(new SlotItemHandler(nodeInventory,
                TransporterNodeBlockEntity.SLOT_CACHE, CACHE_X, CACHE_Y));
        // 过滤器槽
        this.addSlot(new UpgradeSlot(nodeInventory,
                TransporterNodeBlockEntity.SLOT_FILTER, FILTER_X, FILTER_Y));
        // 速度升级槽
        this.addSlot(new SpeedUpgradeSlot(nodeInventory,
                TransporterNodeBlockEntity.SLOT_SPEED, SPEED_X, SPEED_Y));
        // 预留槽（禁用）
        this.addSlot(new LockedSlot(nodeInventory,
                TransporterNodeBlockEntity.SLOT_RESERVED, RESERVED_X, RESERVED_Y));
        // 方向槽：E S W N U D
        //for (int i = 0; i < TransporterNodeBlockEntity.DIR_SLOT_COUNT; i++) {
        //    this.addSlot(new RedstoneTorchSlot(nodeInventory,
        //            TransporterNodeBlockEntity.SLOT_DIR_START + i, DIR_X[i], DIR_Y));
        //}
        for (int i = 0; i < TransporterNodeBlockEntity.DIR_SLOT_COUNT; i++) {
            this.addSlot(new RedstoneTorchSlot(nodeInventory,
                    TransporterNodeBlockEntity.SLOT_DIR_START + i, DIR_X[i], DIR_Y[i]));
        }

        // 玩家背包 3 行
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(playerInv, col + row * 9 + 9,
                        8 + col * 18, INV_Y + row * 18));
            }
        }
        // 玩家快捷栏
        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(playerInv, col, 8 + col * 18, HOTBAR_Y));
        }
    }

    public ContainerLevelAccess getAccess() {
        return this.access;
    }

    private static class UpgradeSlot extends SlotItemHandler {
        public UpgradeSlot(IItemHandler handler, int index, int x, int y) {
            super(handler, index, x, y);
        }
        @Override
        public boolean mayPlace(ItemStack stack) {
            return stack.getItem() instanceof NodeUpgradeItemFilter;
        }
        @Override
        public int getMaxStackSize() {
            return 1;
        }
    }

    private static class SpeedUpgradeSlot extends SlotItemHandler {
        public SpeedUpgradeSlot(IItemHandler handler, int index, int x, int y) {
            super(handler, index, x, y);
        }
        @Override
        public boolean mayPlace(ItemStack stack) {
            return TransporterNodeBlockEntity.isSpeedUpgrade(stack);
        }
    }

    private static class RedstoneTorchSlot extends SlotItemHandler {
        public RedstoneTorchSlot(IItemHandler handler, int index, int x, int y) {
            super(handler, index, x, y);
        }
        @Override
        public boolean mayPlace(ItemStack stack) {
            return stack.is(Items.REDSTONE_TORCH);
        }
        @Override
        public int getMaxStackSize() {
            return 1;
        }
    }

    private static class LockedSlot extends SlotItemHandler {
        public LockedSlot(IItemHandler handler, int index, int x, int y) {
            super(handler, index, x, y);
        }
        @Override public boolean mayPlace(ItemStack stack) { return TransporterNodeBlockEntity.isStackUpgrade(stack); }
        @Override
        public int getMaxStackSize() {
            return 1;
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stackInSlot = slot.getItem();
            itemstack = stackInSlot.copy();
            final int containerSlots = TransporterNodeBlockEntity.INVENTORY_SIZE;

            if (index < containerSlots) {
                if (!this.moveItemStackTo(stackInSlot, containerSlots, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                boolean moved;
                if (TransporterNodeBlockEntity.isSpeedUpgrade(stackInSlot)) {
                    moved = this.moveItemStackTo(stackInSlot,
                            TransporterNodeBlockEntity.SLOT_SPEED,
                            TransporterNodeBlockEntity.SLOT_SPEED + 1, false);
                } else if (stackInSlot.getItem() instanceof NodeUpgradeItemFilter) {
                    moved = this.moveItemStackTo(stackInSlot,
                            TransporterNodeBlockEntity.SLOT_FILTER,
                            TransporterNodeBlockEntity.SLOT_FILTER + 1, false);
                } else if (stackInSlot.is(Items.REDSTONE_TORCH)) {
                    int start = TransporterNodeBlockEntity.SLOT_DIR_START;
                    int end = start + TransporterNodeBlockEntity.DIR_SLOT_COUNT;
                    moved = this.moveItemStackTo(stackInSlot, start, end, false);
                    if (!moved) {
                        moved = this.moveItemStackTo(stackInSlot,
                                TransporterNodeBlockEntity.SLOT_CACHE,
                                TransporterNodeBlockEntity.SLOT_CACHE + 1, false);
                    }
                } else {
                    moved = this.moveItemStackTo(stackInSlot,
                            TransporterNodeBlockEntity.SLOT_CACHE,
                            TransporterNodeBlockEntity.SLOT_CACHE + 1, false);
                }
                if (!moved) return ItemStack.EMPTY;
            }

            if (stackInSlot.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }

            if (stackInSlot.getCount() == itemstack.getCount()) {
                return ItemStack.EMPTY;
            }
            slot.onTake(player, stackInSlot);
        }
        return itemstack;
    }

    public TransporterNodeBlockEntity getBlockEntity() {
        return blockEntity;
    }

    public double getSpeedMultiplier() {
        return blockEntity != null ? blockEntity.getSpeedMultiplier() : 1.0;
    }


    @Override
    public boolean stillValid(Player player) {
        return access.evaluate((level, pos) ->
                        level.getBlockState(pos).is(DTBlocks.TRANSPORTER_NODE.get())
                                && player.distanceToSqr(pos.getX() + 0.5,
                                pos.getY() + 0.5,
                                pos.getZ() + 0.5) <= 64.0,
                true);
    }
}