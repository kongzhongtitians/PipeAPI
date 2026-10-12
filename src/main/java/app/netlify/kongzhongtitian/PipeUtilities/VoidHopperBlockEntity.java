package app.netlify.kongzhongtitian.PipeUtilities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;

import java.util.List;

public class VoidHopperBlockEntity extends BlockEntity {
    private int cooldown = 0;

    public VoidHopperBlockEntity(BlockPos pos, BlockState state) {
        super(DTBlockEntity.VOID_HOPPER_BLOCK_ENTITY.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, VoidHopperBlockEntity entity) {
        if (level.isClientSide) return;

        entity.cooldown++;
        if (entity.cooldown >= 5) {
            entity.cooldown = 0;
            entity.collectItems();
        }
    }

    private void collectItems() {
        AABB collectionArea = new AABB(
                worldPosition.getX() - 7, worldPosition.getY() - 7, worldPosition.getZ() - 7,
                worldPosition.getX() + 8, worldPosition.getY() + 8, worldPosition.getZ() + 8
        );

        List<ItemEntity> items = level.getEntitiesOfClass(ItemEntity.class, collectionArea);

        BlockPos belowPos = worldPosition.below();
        IItemHandler belowContainer = getContainerAt(belowPos);

        if (belowContainer == null) return;

        for (ItemEntity itemEntity : items) {
            if (itemEntity.isRemoved()) continue;

            ItemStack itemStack = itemEntity.getItem().copy();
            if (itemStack.isEmpty()) continue;

            ItemStack remaining = insertItem(belowContainer, itemStack);

            if (remaining.getCount() < itemStack.getCount()) {
                if (remaining.isEmpty()) {
                    itemEntity.discard(); // 全部插入，移除实体
                } else {
                    itemEntity.setItem(remaining); // 部分插入，更新实体
                }

                break;
            }
        }
    }

    private IItemHandler getContainerAt(BlockPos pos) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity != null) {
            return blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.UP).orElse(null);
        }
        return null;
    }

    private ItemStack insertItem(IItemHandler handler, ItemStack stack) {
        ItemStack remaining = stack.copy();

        for (int i = 0; i < handler.getSlots() && !remaining.isEmpty(); i++) {
            remaining = handler.insertItem(i, remaining, false);
        }

        return remaining;
    }
}