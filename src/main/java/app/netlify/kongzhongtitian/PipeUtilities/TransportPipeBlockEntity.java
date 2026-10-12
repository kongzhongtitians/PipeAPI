package app.netlify.kongzhongtitian.PipeUtilities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class TransportPipeBlockEntity extends BlockEntity {
    // 管道现在只是作为连接器，不需要存储物品
    // 但为了能够被其他设备连接，我们提供一个空的ItemHandler

    private final ItemStackHandler emptyHandler = new ItemStackHandler(0);
    private LazyOptional<IItemHandler> lazyItemHandler = LazyOptional.empty();

    public TransportPipeBlockEntity(BlockPos pos, BlockState state) {
        super(DTBlockEntity.TRANSPORT_PIPE.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, TransportPipeBlockEntity entity) {
        // 管道现在只作为连接器，不需要tick逻辑
    }

    @Override
    public void onLoad() {
        super.onLoad();
        lazyItemHandler = LazyOptional.of(() -> emptyHandler);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        lazyItemHandler.invalidate();
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            // 返回一个空的ItemHandler，表示可以连接但不会存储物品
            return lazyItemHandler.cast();
        }

        return super.getCapability(cap, side);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
    }
}