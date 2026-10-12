package app.netlify.kongzhongtitian.PipeUtilities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

public class TransportPipe extends Block {
    // 六个方向的布尔属性
    public static final BooleanProperty DOWN  = BooleanProperty.create("down");
    public static final BooleanProperty UP    = BooleanProperty.create("up");
    public static final BooleanProperty NORTH = BooleanProperty.create("north");
    public static final BooleanProperty SOUTH = BooleanProperty.create("south");
    public static final BooleanProperty WEST  = BooleanProperty.create("west");
    public static final BooleanProperty EAST  = BooleanProperty.create("east");

    private static final VoxelShape CENTER = box(4, 4, 4, 12, 12, 12);
    private static final VoxelShape DOWN_SHAPE  = box(4, 0, 4, 12, 4, 12);
    private static final VoxelShape UP_SHAPE    = box(4, 12, 4, 12, 16, 12);
    private static final VoxelShape NORTH_SHAPE = box(4, 4, 0, 12, 12, 4);
    private static final VoxelShape SOUTH_SHAPE = box(4, 4, 12, 12, 12, 16);
    private static final VoxelShape WEST_SHAPE  = box(0, 4, 4, 4, 12, 12);
    private static final VoxelShape EAST_SHAPE  = box(12, 4, 4, 16, 12, 12);

    public TransportPipe() {
        super(Properties.of()
                .strength(1.0F)
                .sound(SoundType.GLASS)
                .noOcclusion()
                .dynamicShape());
        // 默认所有方向为 false
        registerDefaultState(this.stateDefinition.any()
                .setValue(DOWN, false)
                .setValue(UP, false)
                .setValue(NORTH, false)
                .setValue(SOUTH, false)
                .setValue(WEST, false)
                .setValue(EAST, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(DOWN, UP, NORTH, SOUTH, WEST, EAST);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        // 初始状态根据周围方块计算
        return this.defaultBlockState()
                .setValue(DOWN,  isConnectedTo(level, pos, Direction.DOWN))
                .setValue(UP,    isConnectedTo(level, pos, Direction.UP))
                .setValue(NORTH, isConnectedTo(level, pos, Direction.NORTH))
                .setValue(SOUTH, isConnectedTo(level, pos, Direction.SOUTH))
                .setValue(WEST,  isConnectedTo(level, pos, Direction.WEST))
                .setValue(EAST,  isConnectedTo(level, pos, Direction.EAST));
    }

    // 当邻位方块发生变化时，更新当前方块的连接状态
    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                  LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        boolean connected = isConnectedTo(level, pos, direction);
        return state.setValue(getPropertyForDirection(direction), connected);
    }

    // 辅助：判断某个方向是否应该连接
    private boolean isConnectedTo(LevelAccessor level, BlockPos pos, Direction dir) {
        BlockPos neighborPos = pos.relative(dir);
        BlockState neighborState = level.getBlockState(neighborPos);
        // 同类型方块、物品节点或流体节点都视为相连
        if (neighborState.is(this)
                || neighborState.is(DTBlocks.TRANSPORTER_NODE.get())
                || neighborState.is(DTBlocks.FLUID_TRANSPORTER_NODE.get())) {
            return true;
        }
        BlockEntity be = level.getBlockEntity(neighborPos);
        if (be != null) {
            LazyOptional<IItemHandler> itemCap =
                    be.getCapability(ForgeCapabilities.ITEM_HANDLER, dir.getOpposite());
            if (itemCap.isPresent()) return true;
            LazyOptional<IFluidHandler> fluidCap =
                    be.getCapability(ForgeCapabilities.FLUID_HANDLER, dir.getOpposite());
            if (fluidCap.isPresent()) return true;
        }
        return false;
    }

    // 根据 Direction 获取对应的 BooleanProperty
    public static boolean isConnected(BlockState state, Direction direction) {
        return state.getValue(getPropertyForDirection(direction));
    }

    private static BooleanProperty getPropertyForDirection(Direction dir) {
        return switch (dir) {
            case DOWN  -> DOWN;
            case UP    -> UP;
            case NORTH -> NORTH;
            case SOUTH -> SOUTH;
            case WEST  -> WEST;
            case EAST  -> EAST;
        };
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        VoxelShape shape = CENTER;
        if (state.getValue(DOWN))  shape = Shapes.or(shape, DOWN_SHAPE);
        if (state.getValue(UP))    shape = Shapes.or(shape, UP_SHAPE);
        if (state.getValue(NORTH)) shape = Shapes.or(shape, NORTH_SHAPE);
        if (state.getValue(SOUTH)) shape = Shapes.or(shape, SOUTH_SHAPE);
        if (state.getValue(WEST))  shape = Shapes.or(shape, WEST_SHAPE);
        if (state.getValue(EAST))  shape = Shapes.or(shape, EAST_SHAPE);
        return shape;
    }
}
