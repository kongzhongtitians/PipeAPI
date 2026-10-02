package gd.rf.kongzhongtitian.PipeAPI;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.*;

public class FluidTransporterNodeBlockEntity extends BlockEntity implements MenuProvider {

    public static final int CAPACITY = 8000;
    public static final int EXTRACT_AMOUNT = 1000;
    private static final double BASE_INTERVAL = 10.0;

    // ===== 新增：升级槽位 =====
    public static final int UPGRADE_SLOTS = 1;
    public static final int SLOT_SPEED = 0;

    private final ItemStackHandler upgradeHandler = new ItemStackHandler(UPGRADE_SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            syncToClient();
        }
        @Override
        public int getSlotLimit(int slot) {
            return 64;  // 允许堆叠多个速度升级
        }
    };

    public ItemStackHandler getUpgradeHandler() {
        return upgradeHandler;
    }
    // ===== 新增结束 =====

    private final FluidTank fluidTank = new FluidTank(CAPACITY) {
        @Override
        protected void onContentsChanged() {
            setChanged();
            syncToClient();
        }
    };

    private LazyOptional<IFluidHandler> lazyFluidHandler = LazyOptional.empty();

    private double extractCooldown = BASE_INTERVAL;

    private List<BlockPos> outputTargets = new ArrayList<>();
    private Map<BlockPos, Integer>   targetDistances = new HashMap<>();
    private Map<BlockPos, Direction> targetFaces     = new HashMap<>();
    private int nextTargetIndex = 0;

    private boolean isTransitActive = false;
    private BlockPos currentTarget = null;
    private Direction currentTargetFace = null;
    private double currentRemainingTicks = 0.0;

    public FluidTransporterNodeBlockEntity(BlockPos pos, BlockState state) {
        super(DTBlockEntity.FLUID_TRANSPORTER_NODE.get(), pos, state);
    }

    public FluidTank getFluidTank() {
        return fluidTank;
    }

    /* ==================== 速度升级 ==================== */

    public static boolean isSpeedUpgrade(ItemStack stack) {
        if (stack.isEmpty()) return false;
        ResourceLocation rl = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (rl == null) return false;
        String id = rl.toString();
        return "exura:upgrade_speed".equals(id)
                || "exura:upgrade_speed_enchanted".equals(id)
                || "exura:upgrade_speed_super".equals(id);
    }

    /** 与物品节点一致：每个物品叠乘一个 < 1 的系数，多个物品整体叠加 */
    private double getSpeedMultiplier() {
        ItemStack stack = upgradeHandler.getStackInSlot(SLOT_SPEED);
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

    /** 每次抽取的冷却 tick 数（基础 10 tick × 速度倍率） */
    private double getExtractInterval() {
        return BASE_INTERVAL * getSpeedMultiplier();
    }

    // ====== 供 UI 展示用 ======
    public double getSpeedMultiplierPublic() {
        return getSpeedMultiplier();
    }

    /* ==================== Tick ==================== */

    public static void tick(Level level, BlockPos pos, BlockState state, FluidTransporterNodeBlockEntity node) {
        if (level.isClientSide) return;

        node.processTransit();

        node.extractCooldown -= 1.0;
        if (node.extractCooldown <= 0.0) {
            // 改动点：用 getExtractInterval() 替代 BASE_INTERVAL
            node.extractCooldown += node.getExtractInterval();
            node.refreshNetwork();
            node.tryExtractFluidToTank();
        }
        node.tryStartTransit();
    }

    /* ==================== 传输逻辑 ==================== */

    private void processTransit() {
        if (!isTransitActive) return;

        if (currentRemainingTicks > 0.0) {
            currentRemainingTicks -= 1.0;
            return;
        }

        if (fluidTank.getFluidAmount() <= 0) {
            isTransitActive = false;
            currentTarget = null;
            currentTargetFace = null;
            return;
        }

        if (currentTarget == null || !level.isLoaded(currentTarget)) {
            isTransitActive = false;
            currentTarget = null;
            currentTargetFace = null;
            return;
        }

        BlockEntity targetBe = level.getBlockEntity(currentTarget);
        if (targetBe == null) {
            isTransitActive = false;
            currentTarget = null;
            currentTargetFace = null;
            return;
        }

        IFluidHandler targetHandler = findFillableHandler(targetBe, currentTargetFace);
        if (targetHandler == null) {
            isTransitActive = false;
            currentTarget = null;
            currentTargetFace = null;
            return;
        }

        FluidStack contents = fluidTank.getFluid();
        if (contents.isEmpty()) {
            isTransitActive = false;
            currentTarget = null;
            currentTargetFace = null;
            return;
        }

        int filled = targetHandler.fill(contents.copy(), IFluidHandler.FluidAction.EXECUTE);
        if (filled > 0) {
            fluidTank.drain(filled, IFluidHandler.FluidAction.EXECUTE);
        }

        if (filled <= 0 || fluidTank.getFluidAmount() <= 0) {
            // 目标拒收或已全部送出：结束本次传输
            isTransitActive = false;
            currentTarget = null;
            currentTargetFace = null;
        } else {
            // 只送出一部分：重置并立即尝试下一个目标，避免死磕同一目标
            isTransitActive = false;
            currentTarget = null;
            currentTargetFace = null;
            tryStartTransit();
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
        Map<BlockPos, Direction> faces = new HashMap<>();
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
                } else {
                    BlockEntity be = level.getBlockEntity(adjacent);
                    if (be == null || containers.containsKey(adjacent)) continue;
                    Direction facing = dir.getOpposite();
                    if (findFillableHandler(be, facing) != null) {
                        containers.put(adjacent, currentDist);
                        faces.put(adjacent, facing);
                    }
                }
            }
        }

        this.targetFaces = faces;
        return containers;
    }

    /* ==================== 抽取逻辑 ==================== */

    private void tryExtractFluidToTank() {
        if (fluidTank.getFluidAmount() >= fluidTank.getCapacity()) return;
        int space = fluidTank.getCapacity() - fluidTank.getFluidAmount();
        int maxExtract = Math.min(EXTRACT_AMOUNT, space);
        if (maxExtract <= 0) return;

        for (Direction dir : Direction.values()) {
            BlockPos neighbor = worldPosition.relative(dir);
            if (!level.isLoaded(neighbor)) continue;

            BlockState neighborState = level.getBlockState(neighbor);
            if (neighborState.is(DTBlocks.FLUID_TRANSPORTER_NODE.get())
                    || neighborState.is(DTBlocks.TRANSPORT_PIPE.get())) {
                continue;
            }

            BlockEntity be = level.getBlockEntity(neighbor);
            if (be == null) continue;

            IFluidHandler handler = findExtractableHandler(be, dir.getOpposite());
            if (handler == null) continue;

            if (tryTransferFromHandler(handler, maxExtract)) {
                return;
            }
        }
    }

    @Nullable
    private IFluidHandler findExtractableHandler(BlockEntity be, @Nullable Direction face) {
        if (face != null) {
            IFluidHandler h = be.getCapability(ForgeCapabilities.FLUID_HANDLER, face).orElse(null);
            if (h != null && canExtractFrom(h)) return h;
        }
        IFluidHandler h = be.getCapability(ForgeCapabilities.FLUID_HANDLER, null).orElse(null);
        if (h != null && canExtractFrom(h)) return h;
        for (Direction dir : Direction.values()) {
            h = be.getCapability(ForgeCapabilities.FLUID_HANDLER, dir).orElse(null);
            if (h != null && canExtractFrom(h)) return h;
        }
        return null;
    }

    @Nullable
    private IFluidHandler findFillableHandler(BlockEntity be, @Nullable Direction face) {
        if (face != null) {
            IFluidHandler h = be.getCapability(ForgeCapabilities.FLUID_HANDLER, face).orElse(null);
            if (h != null && canFillTo(h)) return h;
        }
        IFluidHandler h = be.getCapability(ForgeCapabilities.FLUID_HANDLER, null).orElse(null);
        if (h != null && canFillTo(h)) return h;
        for (Direction dir : Direction.values()) {
            h = be.getCapability(ForgeCapabilities.FLUID_HANDLER, dir).orElse(null);
            if (h != null && canFillTo(h)) return h;
        }
        return null;
    }

    private boolean canExtractFrom(IFluidHandler h) {
        try {
            for (int tank = 0; tank < h.getTanks(); tank++) {
                FluidStack inTank = h.getFluidInTank(tank);
                if (inTank.isEmpty()) continue;
                FluidStack probe = inTank.copy();
                probe.setAmount(1);
                if (!h.drain(probe, IFluidHandler.FluidAction.SIMULATE).isEmpty()) {
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean canFillTo(IFluidHandler h) {
        try {
            for (int tank = 0; tank < h.getTanks(); tank++) {
                FluidStack existing = h.getFluidInTank(tank);
                if (existing.isEmpty()) continue;
                FluidStack probe = existing.copy();
                probe.setAmount(1);
                if (h.fill(probe, IFluidHandler.FluidAction.SIMULATE) > 0) return true;
            }
            FluidStack buffer = fluidTank.getFluid();
            if (!buffer.isEmpty()) {
                FluidStack probe = buffer.copy();
                probe.setAmount(1);
                if (h.fill(probe, IFluidHandler.FluidAction.SIMULATE) > 0) return true;
            }
            // 修复 #5：不再仅凭“存在空槽”判定可填充。
            // 空槽必须能接受本节点待传输的流体类型（由上方 SIMULATE fill 校验），
            // 否则会把只接受其他流体类型的空容器误判为目标，导致 fill 失败、传输中断。
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean tryTransferFromHandler(IFluidHandler handler, int maxExtract) {
        FluidStack simulated = handler.drain(maxExtract, IFluidHandler.FluidAction.SIMULATE);
        if (simulated.isEmpty()) return false;

        int accepted = fluidTank.fill(simulated, IFluidHandler.FluidAction.SIMULATE);
        if (accepted <= 0) return false;

        FluidStack drained = handler.drain(accepted, IFluidHandler.FluidAction.EXECUTE);
        if (drained.isEmpty()) return false;

        fluidTank.fill(drained, IFluidHandler.FluidAction.EXECUTE);
        return true;
    }

    private void tryStartTransit() {
        if (isTransitActive) return;
        if (outputTargets.isEmpty()) return;
        if (fluidTank.getFluidAmount() <= 0) return;

        BlockPos target = outputTargets.get(nextTargetIndex % outputTargets.size());
        nextTargetIndex = (nextTargetIndex + 1) % outputTargets.size();

        int distance = targetDistances.getOrDefault(target, 0);
        // 改动点：传输延迟同样应用速度倍率
        currentRemainingTicks = distance * BASE_INTERVAL * getSpeedMultiplier();

        isTransitActive = true;
        currentTarget = target;
        currentTargetFace = targetFaces.get(target);
    }

    /* ==================== 掉落 ==================== */

    /** 方块被破坏时掉落升级物品（与物品节点 dropContents 类似） */
    public void dropContents(Level level, BlockPos pos) {
        for (int i = 0; i < upgradeHandler.getSlots(); i++) {
            ItemStack stack = upgradeHandler.getStackInSlot(i);
            if (!stack.isEmpty()) {
                Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, stack);
                upgradeHandler.setStackInSlot(i, ItemStack.EMPTY);
            }
        }
        // 储罐流体破坏时直接消失：不转桶掉落，避免凭空产出桶（白嫖/刷桶漏洞）。
        // 与多数物流模组的缓存内容物处理方式一致。
    }

    /* ==================== 能力 / 同步 ==================== */

    @Override
    public void onLoad() {
        super.onLoad();
        lazyFluidHandler = LazyOptional.of(() -> fluidTank);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        lazyFluidHandler.invalidate();
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.FLUID_HANDLER) {
            return lazyFluidHandler.cast();
        }
        return super.getCapability(cap, side);
    }

    private void syncToClient() {
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    public CompoundTag getUpdateTag() {
        return saveWithoutMetadata();
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("fluid", fluidTank.writeToNBT(new CompoundTag()));
        // 新增：升级槽位
        tag.put("upgrades", upgradeHandler.serializeNBT());
        tag.putBoolean("isTransitActive", isTransitActive);
        if (currentTarget != null) {
            tag.putLong("currentTarget", currentTarget.asLong());
        }
        tag.putDouble("currentRemainingTicks", currentRemainingTicks);
        tag.putDouble("extractCooldown", extractCooldown);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("fluid")) {
            fluidTank.readFromNBT(tag.getCompound("fluid"));
        }
        if (tag.contains("upgrades")) {
            upgradeHandler.deserializeNBT(tag.getCompound("upgrades"));
        }
        isTransitActive = tag.getBoolean("isTransitActive");
        if (tag.contains("currentTarget")) {
            currentTarget = BlockPos.of(tag.getLong("currentTarget"));
        }
        currentRemainingTicks = tag.getDouble("currentRemainingTicks");
        extractCooldown = tag.contains("extractCooldown") ? tag.getDouble("extractCooldown") : BASE_INTERVAL;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.pipe_api.fluid_transporter_node");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new FluidTransporterNodeMenu(id, inv, this,
                ContainerLevelAccess.create(level, worldPosition));
    }
}