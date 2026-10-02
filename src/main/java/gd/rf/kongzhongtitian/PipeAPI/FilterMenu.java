package gd.rf.kongzhongtitian.PipeAPI;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;

import java.util.ArrayList;
import java.util.List;

public class FilterMenu extends AbstractContainerMenu {
    private final ItemStack upgradeStack;
    private final ItemStackHandler filterHandler;
    private static final int FILTER_SLOT_COUNT = 9;

    public FilterMenu(int id, Inventory playerInv, ItemStack upgradeStack) {
        super(DTMenu.FILTER_MENU.get(), id);
        this.upgradeStack = upgradeStack;

        List<ItemStack> filters = NodeUpgradeItemFilter.getFilterItems(upgradeStack);
        this.filterHandler = new ItemStackHandler(FILTER_SLOT_COUNT) {
            @Override
            protected void onContentsChanged(int slot) {
                saveToItem();
            }
        };
        // 填充已有过滤物品
        for (int i = 0; i < FILTER_SLOT_COUNT && i < filters.size(); i++) {
            filterHandler.setStackInSlot(i, filters.get(i));
        }

        // 过滤槽 (3x3) —— 禁止直接放入和取出
        for (int i = 0; i < FILTER_SLOT_COUNT; i++) {
            int row = i / 3;
            int col = i % 3;
            int x = 62 + col * 18;
            int y = 17 + row * 18;  // 上移1像素
            this.addSlot(new SlotItemHandler(filterHandler, i, x, y) {
                @Override
                public boolean mayPlace(ItemStack stack) { return false; }
                @Override
                public boolean mayPickup(Player player) { return false; }
            });
        }

        // 玩家背包 (3行)
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(playerInv, col + row * 9 + 9,
                        8 + col * 18, 84 + row * 18));
            }
        }
        // 快捷栏 (1行)
        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(playerInv, col,
                    8 + col * 18, 142));
        }
    }

    private void saveToItem() {
        List<ItemStack> filters = new ArrayList<>();
        for (int i = 0; i < filterHandler.getSlots(); i++) {
            ItemStack stack = filterHandler.getStackInSlot(i);
            if (!stack.isEmpty()) {
                filters.add(stack);
            }
        }
        NodeUpgradeItemFilter.setFilterItems(upgradeStack, filters);
    }

    @Override
    public boolean stillValid(Player player) {
        // 玩家主手/副手仍持有该过滤器物品时菜单才保持打开（#10）
        return isHoldingFilter(player.getMainHandItem())
                || isHoldingFilter(player.getOffhandItem());
    }

    private boolean isHoldingFilter(ItemStack stack) {
        return stack.getItem() instanceof NodeUpgradeItemFilter
                && ItemStack.isSameItemSameTags(stack, upgradeStack);
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        // 只处理过滤槽（0~8）的点击
        if (slotId >= 0 && slotId < FILTER_SLOT_COUNT) {
            // 左键点击（PICKUP + button 0）且有物品 -> 删除该物品
            if (clickType == ClickType.PICKUP && button == 0) {
                ItemStack current = filterHandler.getStackInSlot(slotId);
                if (!current.isEmpty()) {
                    filterHandler.setStackInSlot(slotId, ItemStack.EMPTY);
                    // onContentsChanged 会自动调用 saveToItem，此处无需额外调用
                }
            }
            // 其他点击（右键、拖拽等）一律忽略
            return;
        }
        // 非过滤槽的点击交给父类处理
        super.clicked(slotId, button, clickType, player);
    }

    @Override
    protected boolean moveItemStackTo(ItemStack stack, int startIndex, int endIndex, boolean reverse) {
        // 目标为过滤槽（0~8）
        if (startIndex == 0 && endIndex == 9) {
            // 检查是否已存在相同物品
            for (int i = 0; i < 9; i++) {
                ItemStack existing = this.slots.get(i).getItem();
                if (!existing.isEmpty() && ItemStack.isSameItem(stack, existing)) {
                    return false; // 已存在，不重复添加
                }
            }
            // 否则放入第一个空槽
            for (int i = 0; i < 9; i++) {
                Slot slot = this.slots.get(i);
                if (!slot.hasItem()) {
                    ItemStack copy = stack.copy();
                    copy.setCount(1);
                    slot.set(copy);
                    return true;
                }
            }
            return false; // 无空槽
        }
        return super.moveItemStackTo(stack, startIndex, endIndex, reverse);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = this.slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        // 如果是过滤槽，不允许移出
        if (index < FILTER_SLOT_COUNT) {
            return ItemStack.EMPTY;
        }
        // 否则尝试复制到过滤槽
        if (!this.moveItemStackTo(stack, 0, FILTER_SLOT_COUNT, false)) {
            return ItemStack.EMPTY;
        }
        // 源物品不消耗，返回一个副本表示操作成功
        return stack.copy();
    }
}
