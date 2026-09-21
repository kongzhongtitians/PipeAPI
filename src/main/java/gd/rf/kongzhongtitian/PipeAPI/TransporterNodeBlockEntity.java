package gd.rf.kongzhongtitian.PipeAPI;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.*;


public class TransporterNodeBlockEntity extends BlockEntity implements MenuProvider {

    // ==== 槽位布局（共 10 格）====
    // 0  缓存
    // 1  过滤器
    // 2  速度升级
    // 3  预留（禁用）
    // 4~9 方向：E S W N U D
    public static final int INVENTORY_SIZE = 10;
    public static final int SLOT_CACHE    = 0;
    public static final int SLOT_FILTER   = 1;
    public static final int SLOT_SPEED    = 2;
    public static final int SLOT_RESERVED = 3;
    public static final int SLOT_DIR_START = 4;
    public static final int DIR_SLOT_COUNT = 6;

    // 方向顺序：东、南、西、北、上、下
    public static final Direction[] DIRECTION_ORDER = {
            Direction.EAST, Direction.SOUTH, Direction.WEST,
            Direction.NORTH, Direction.UP, Direction.DOWN
    };
	
    private final ItemStackHandler itemHandler = new ItemStackHandler(INVENTORY_SIZE) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };
    
	public IItemHandler getItemHandler() {
		return itemHandler;
	}
	
    private LazyOptional<IItemHandler> lazyHandler = LazyOptional.empty();

    private double extractCooldown = 10.0;

    private List<BlockPos> outputTargets = new ArrayList<>();
    private Map<BlockPos, Integer> targetDistances = new HashMap<>();
    private int nextTargetIndex = 0;

    private boolean isTransitActive = false;
    private BlockPos currentTarget = null;
    private double currentRemainingTicks = 0.0;

    public TransporterNodeBlockEntity(BlockPos pos, BlockState state) {
        super(DTBlockEntity.TRANSPORTER_NODE.get(), pos, state);
    }

    // ---- 方向槽工具方法 ----

    public static int getSlotForDirection(Direction dir) {
        for (int i = 0; i < DIRECTION_ORDER.length; i++) {
            if (DIRECTION_ORDER[i] == dir) return SLOT_DIR_START + i;
        }
        return -1;
    }

    public static Direction getDirectionForSlot(int slot) {
        int idx = slot - SLOT_DIR_START;
        if (idx >= 0 && idx < DIR_SLOT_COUNT) return DIRECTION_ORDER[idx];
        return null;
    }

    /** 该方向是否放入了红石火把（启用） */
    public boolean isDirectionEnabled(Direction dir) {
        int slot = getSlotForDirection(dir);
        if (slot < 0) return false;
        ItemStack stack = itemHandler.getStackInSlot(slot);
        return !stack.isEmpty() && stack.is(Items.REDSTONE_TORCH);
    }

    // ---- Tick ----

    public static void tick(Level level, BlockPos pos, BlockState state, TransporterNodeBlockEntity node) {
        if (level.isClientSide) return;

        node.processTransit();

        node.extractCooldown -= 1.0;
        if (node.extractCooldown <= 0.0) {
            node.extractCooldown += node.getExtractInterval();
            node.refreshNetwork();
            node.tryExtractOneToCache();
            node.tryStartTransit();
        }
    }

    private void processTransit() {
        if (!isTransitActive) return;

        if (currentRemainingTicks > 0.0) {
            currentRemainingTicks -= 1.0;
            return;
        }

        ItemStack cacheStack = itemHandler.getStackInSlot(SLOT_CACHE);
        if (cacheStack.isEmpty()) {
            isTransitActive = false;
            currentTarget = null;
            return;
        }

        if (currentTarget == null || !level.isLoaded(currentTarget)) {
            switchToNextTarget();
            return;
        }

        BlockEntity targetBe = level.getBlockEntity(currentTarget);
        if (targetBe == null) {
            switchToNextTarget();
            return;
        }

        LazyOptional<IItemHandler> cap = targetBe.getCapability(ForgeCapabilities.ITEM_HANDLER);
        if (!cap.isPresent()) {
            switchToNextTarget();
            return;
        }

        IItemHandler handler = cap.orElse(null);
        if (handler == null) {
            switchToNextTarget();
            return;
        }

        ItemStack leftover = ItemHandlerHelper.insertItem(handler, cacheStack.copy(), false);

        if (leftover.isEmpty()) {
            // 全部插入成功
            itemHandler.setStackInSlot(SLOT_CACHE, ItemStack.EMPTY);
            isTransitActive = false;
            currentTarget = null;
        } else {
            // 目标满或只能插入一部分，剩余物品放回缓存，然后切换下一个目标
            itemHandler.setStackInSlot(SLOT_CACHE, leftover);
            switchToNextTarget();
        }
    }

    private void refreshNetwork() {
        Map<BlockPos, Integer> result = searchNetwork();
        outputTargets = new ArrayList<>(result.keySet());
        targetDistances = result;
        if (outputTargets.isEmpty()) {
            nextTargetIndex = 0;
        }
    }

    private Map<BlockPos, Integer> searchNetwork() {
        Map<BlockPos, Integer> containers = new LinkedHashMap<>();
        Set<BlockPos> visited = new HashSet<>();
        Queue<BlockPos> queue = new LinkedList<>();
        Map<BlockPos, Integer> distances = new HashMap<>();

        for (Direction dir : Direction.values()) {
            BlockPos neighbor = worldPosition.relative(dir);
            BlockState neighborState = level.getBlockState(neighbor);
            if (neighborState.is(DTBlocks.TRANSPORT_PIPE.get())
                    && TransportPipe.isConnected(neighborState, dir.getOpposite())) {
                if (visited.add(neighbor)) {
                    queue.add(neighbor);
                    distances.put(neighbor, 1);
                }
            }
        }

        int maxSearch = 200;
        while (!queue.isEmpty() && maxSearch-- > 0) {
            BlockPos current = queue.poll();
            int currentDist = distances.get(current);
            BlockState currentState = level.getBlockState(current);
            for (Direction dir : Direction.values()) {
                if (!TransportPipe.isConnected(currentState, dir)) continue;
                BlockPos adjacent = current.relative(dir);
                if (adjacent.equals(worldPosition)) continue;

                BlockState adjState = level.getBlockState(adjacent);
                if (adjState.is(DTBlocks.TRANSPORT_PIPE.get())
                        && TransportPipe.isConnected(adjState, dir.getOpposite())) {
                    if (visited.add(adjacent)) {
                        queue.add(adjacent);
                        distances.put(adjacent, currentDist + 1);
                    }
                } else if (!adjState.is(DTBlocks.TRANSPORTER_NODE.get()) && !adjState.is(DTBlocks.FLUID_TRANSPORTER_NODE.get()) && level.getBlockEntity(adjacent) != null) {
                    BlockEntity be = level.getBlockEntity(adjacent);
                    if (be != null && !containers.containsKey(adjacent)) {
                        LazyOptional<IItemHandler> cap = be.getCapability(ForgeCapabilities.ITEM_HANDLER, dir.getOpposite());
                        if (cap.isPresent()) {
                            containers.put(adjacent, currentDist);
                        }
                    }
                }
            }
        }
        return containers;
    }

    private List<ItemStack> getFilterItems() {
        ItemStack upgrade = itemHandler.getStackInSlot(SLOT_FILTER);
        if (!upgrade.isEmpty() && upgrade.getItem() instanceof NodeUpgradeItemFilter) {
            return NodeUpgradeItemFilter.getFilterItems(upgrade);
        }
        return List.of();
    }

    public static boolean isSpeedUpgrade(ItemStack stack) {
        if (stack.isEmpty()) return false;
        ResourceLocation rl = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (rl == null) return false;
        String id = rl.toString();
        return "exura:upgrade_speed".equals(id)
                || "exura:upgrade_speed_enchanted".equals(id)
                || "exura:upgrade_speed_super".equals(id);
    }

    public static boolean isStackUpgrade(ItemStack stack) {
        if (stack.isEmpty()) return false;
        ResourceLocation rl = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (rl == null) return false;
        String id = rl.toString();
        return "exura:upgrade_stack".equals(id);
    }

    private double getSpeedMultiplier() {
        ItemStack stack = itemHandler.getStackInSlot(SLOT_SPEED);
        if (stack.isEmpty() || !isSpeedUpgrade(stack)) return 1.0;

        ResourceLocation rl = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (rl == null) return 1.0;
        String id = rl.toString();

        double perItem;
        switch (id) {
            case "exura:upgrade_speed":           perItem = 0.5;  break;
            case "exura:upgrade_speed_enchanted": perItem = 0.49; break;
            case "exura:upgrade_speed_super":     perItem = 0.48; break;
            default: return 1.0;
        }

        double multiplier = 1.0;
        int count = stack.getCount();
        for (int i = 0; i < count; i++) {
            multiplier *= perItem;
        }
        return multiplier;
    }

    private boolean getStackMultiplier() {
        ItemStack stack = itemHandler.getStackInSlot(SLOT_RESERVED);
        if (stack.isEmpty() || !isStackUpgrade(stack)) return false;

        ResourceLocation rl = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (rl == null) return false;
        String id = rl.toString();

        return "exura:upgrade_stack".equals(id);
    }

    private double getExtractInterval() {
        return 10.0 * getSpeedMultiplier();
    }

    private void tryExtractOneToCache() {
        List<ItemStack> filters = getFilterItems();
        boolean stackUpgrade = getStackMultiplier(); // 是否启用了整组抽取

        for (Direction dir : DIRECTION_ORDER) {
            if (!isDirectionEnabled(dir)) continue;

            BlockPos neighbor = worldPosition.relative(dir);
            if (!level.isLoaded(neighbor)) continue;

            BlockEntity be = level.getBlockEntity(neighbor);
            if (be == null) continue;

            if (level.getBlockState(neighbor).is(DTBlocks.TRANSPORTER_NODE.get())
                    || level.getBlockState(neighbor).is(DTBlocks.TRANSPORT_PIPE.get())) continue;

            LazyOptional<IItemHandler> capOptional = be.getCapability(ForgeCapabilities.ITEM_HANDLER, dir.getOpposite());
            if (!capOptional.isPresent()) continue;

            IItemHandler handler = capOptional.orElse(null);
            for (int slot = 0; slot < handler.getSlots(); slot++) {
                ItemStack stackInSlot = handler.getStackInSlot(slot);
                if (stackInSlot.isEmpty()) continue;

                if (!filters.isEmpty()) {
                    boolean matches = false;
                    for (ItemStack filter : filters) {
                        if (ItemStack.isSameItem(stackInSlot, filter)) {
                            matches = true;
                            break;
                        }
                    }
                    if (!matches) continue;
                }

                int maxExtract = stackUpgrade ? stackInSlot.getMaxStackSize() : 1;

                ItemStack simulatedExtract = handler.extractItem(slot, maxExtract, true);
                if (simulatedExtract.isEmpty()) continue;

                ItemStack cacheStack = itemHandler.getStackInSlot(SLOT_CACHE);
                int space;
                if (cacheStack.isEmpty()) {
                    space = simulatedExtract.getMaxStackSize();
                } else if (ItemStack.isSameItemSameTags(cacheStack, simulatedExtract)) {
                    space = cacheStack.getMaxStackSize() - cacheStack.getCount();
                } else {
                    continue;
                }

                if (space <= 0) continue;

                int extractCount = Math.min(simulatedExtract.getCount(), space);

                ItemStack extracted = handler.extractItem(slot, extractCount, false);
                if (extracted.isEmpty()) continue;

                ItemStack remainder = itemHandler.insertItem(SLOT_CACHE, extracted, false);
                if (!remainder.isEmpty()) {
                    handler.insertItem(slot, remainder, false);
                }
                return;
            }
        }
    }

    private void tryStartTransit() {
        if (isTransitActive) return;
        if (outputTargets.isEmpty()) return;

        ItemStack cacheStack = itemHandler.getStackInSlot(SLOT_CACHE);
        if (cacheStack.isEmpty()) return;

        BlockPos target = outputTargets.get(nextTargetIndex % outputTargets.size());
        nextTargetIndex = (nextTargetIndex + 1) % outputTargets.size();

        int distance = targetDistances.getOrDefault(target, 0);
        currentRemainingTicks = distance * 10.0 * getSpeedMultiplier();

        isTransitActive = true;
        currentTarget = target;
    }

    public void dropContents(Level level, BlockPos pos) {
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            ItemStack stack = itemHandler.getStackInSlot(i);
            if (!stack.isEmpty()) {
                Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), stack);
                itemHandler.setStackInSlot(i, ItemStack.EMPTY);
            }
        }
        isTransitActive = false;
        currentTarget = null;
        currentRemainingTicks = 0.0;
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        lazyHandler.invalidate();
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            return lazyHandler.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
	public void onLoad() {
		super.onLoad();
		lazyHandler = LazyOptional.of(() -> new SingleSlotItemHandler(itemHandler, SLOT_CACHE));
	}

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("inventory", itemHandler.serializeNBT());
        tag.putBoolean("isTransitActive", isTransitActive);
        if (currentTarget != null) {
            tag.putLong("currentTarget", currentTarget.asLong());
        }
        tag.putDouble("currentRemainingTicks", currentRemainingTicks);
        tag.putDouble("extractCooldown", extractCooldown);
    }

    private void switchToNextTarget() {
        isTransitActive = false;
        currentTarget = null;
        tryStartTransit();
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        itemHandler.deserializeNBT(tag.getCompound("inventory"));
        isTransitActive = tag.getBoolean("isTransitActive");
        if (tag.contains("currentTarget")) {
            currentTarget = BlockPos.of(tag.getLong("currentTarget"));
        }
        currentRemainingTicks = tag.getDouble("currentRemainingTicks");
        extractCooldown = tag.contains("extractCooldown") ? tag.getDouble("extractCooldown") : 10.0;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.pipe_api.transporter_node");
    }

    @Nullable
    @Override
	public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
		return new TransporterNodeMenu(id, inv, this.itemHandler,
				ContainerLevelAccess.create(level, worldPosition));
	}
}