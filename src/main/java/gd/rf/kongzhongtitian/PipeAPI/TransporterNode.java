package gd.rf.kongzhongtitian.PipeAPI;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;

import javax.annotation.Nullable;

public class TransporterNode extends Block implements EntityBlock {
    public TransporterNode() {
        super(Properties.of()
                .strength(2.0F)
                .sound(SoundType.METAL)
                .noOcclusion());
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TransporterNodeBlockEntity(pos, state);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof TransporterNodeBlockEntity node) {
                NetworkHooks.openScreen((ServerPlayer) player, node, pos);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (type == DTBlockEntity.TRANSPORTER_NODE.get()) {
            return (level1, pos1, state1, be) -> {
                TransporterNodeBlockEntity.tick(level1, pos1, state1, (TransporterNodeBlockEntity) be);
            };
        }
        return null;
    }

	@Override
	public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
		if (!state.is(newState.getBlock())) {
			BlockEntity blockEntity = level.getBlockEntity(pos);
			if (blockEntity instanceof TransporterNodeBlockEntity node) {
				node.dropContents(level, pos);
			}
			level.updateNeighbourForOutputSignal(pos, this);
			super.onRemove(state, level, pos, newState, isMoving);
		}
	}
}