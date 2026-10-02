package gd.rf.kongzhongtitian.PipeAPI;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;

public class FluidTransporterNodeMenu extends AbstractContainerMenu {

    private final ContainerLevelAccess access;
    private final FluidTransporterNodeBlockEntity blockEntity;

    // 容器侧槽位数量（此节点只有 1 个升级槽）
    public static final int CONTAINER_SLOTS = 1;

    public FluidTransporterNodeMenu(int windowId, Inventory playerInv,
                                    FluidTransporterNodeBlockEntity blockEntity,
                                    ContainerLevelAccess access) {
        super(DTMenu.FLUID_TRANSPORTER_NODE_MENU.get(), windowId);
        this.access = access;
        this.blockEntity = blockEntity;

        // ===== 升级槽位（放在 GUI 右上角） =====
        // 修复 A：blockEntity 可能为 null（DTMenu 注册处未提供 fallback），
        // 此时使用临时 handler 兜底，避免 NPE
        IItemHandler upgradeHandler = blockEntity != null
                ? blockEntity.getUpgradeHandler()
                : new ItemStackHandler(FluidTransporterNodeBlockEntity.UPGRADE_SLOTS);
        this.addSlot(new SpeedUpgradeSlot(
                upgradeHandler,
                FluidTransporterNodeBlockEntity.SLOT_SPEED,
                134, 20));

        // 玩家背包（27 格）
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(playerInv, col + row * 9 + 9,
                        8 + col * 18, 84 + row * 18));
            }
        }
        // 快捷栏
        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(playerInv, col, 8 + col * 18, 142));
        }
    }

    // 只允许放速度升级物品
    private static class SpeedUpgradeSlot extends SlotItemHandler {
        public SpeedUpgradeSlot(IItemHandler handler, int index, int x, int y) {
            super(handler, index, x, y);
        }
        @Override
        public boolean mayPlace(ItemStack stack) {
            return FluidTransporterNodeBlockEntity.isSpeedUpgrade(stack);
        }
    }

    public FluidTransporterNodeBlockEntity getBlockEntity() {
        return blockEntity;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) return ItemStack.EMPTY;

        ItemStack stackInSlot = slot.getItem();
        itemstack = stackInSlot.copy();

        if (index < CONTAINER_SLOTS) {
            // 从容器槽 → 玩家背包
            if (!this.moveItemStackTo(stackInSlot, CONTAINER_SLOTS, this.slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else {
            // 从玩家背包 → 容器槽（只有速度升级能进）
            if (!FluidTransporterNodeBlockEntity.isSpeedUpgrade(stackInSlot)) {
                return ItemStack.EMPTY;
            }
            if (!this.moveItemStackTo(stackInSlot, 0, CONTAINER_SLOTS, false)) {
                return ItemStack.EMPTY;
            }
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
        return itemstack;
    }

    @Override
    public boolean stillValid(Player player) {
        return access.evaluate((level, pos) ->
                        level.getBlockState(pos).is(DTBlocks.FLUID_TRANSPORTER_NODE.get())
                                && player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64.0,
                true);
    }
}