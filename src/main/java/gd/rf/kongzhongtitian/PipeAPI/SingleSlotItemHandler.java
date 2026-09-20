package gd.rf.kongzhongtitian.PipeAPI;

import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandlerModifiable;

public class SingleSlotItemHandler implements IItemHandlerModifiable {
    private final IItemHandlerModifiable delegate;
    private final int exposedSlot;

    public SingleSlotItemHandler(IItemHandlerModifiable delegate, int exposedSlot) {
        this.delegate = delegate;
        this.exposedSlot = exposedSlot;
    }

    @Override
    public int getSlots() {
        return 1;
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        if (slot != 0) return ItemStack.EMPTY;
        return delegate.getStackInSlot(exposedSlot);
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        if (slot != 0) return stack;
        return delegate.insertItem(exposedSlot, stack, simulate);
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (slot != 0) return ItemStack.EMPTY;
        return delegate.extractItem(exposedSlot, amount, simulate);
    }

    @Override
    public int getSlotLimit(int slot) {
        if (slot != 0) return 0;
        return delegate.getSlotLimit(exposedSlot);
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        if (slot != 0) return false;
        return delegate.isItemValid(exposedSlot, stack);
    }

    @Override
    public void setStackInSlot(int slot, ItemStack stack) {
        if (slot == 0) {
            delegate.setStackInSlot(exposedSlot, stack);
        }
    }
}