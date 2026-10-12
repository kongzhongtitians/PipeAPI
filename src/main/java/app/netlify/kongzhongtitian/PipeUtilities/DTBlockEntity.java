package app.netlify.kongzhongtitian.PipeUtilities;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class DTBlockEntity {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, PipeAPI.MODID);

    public static final RegistryObject<BlockEntityType<VoidHopperBlockEntity>> VOID_HOPPER_BLOCK_ENTITY =
            BLOCK_ENTITY_TYPES.register("void_hopper_block_entity", () -> BlockEntityType.Builder.of(
                    VoidHopperBlockEntity::new, DTBlocks.VOID_HOPPER.get()).build(null));

    public static final RegistryObject<BlockEntityType<TransporterNodeBlockEntity>> TRANSPORTER_NODE =
            BLOCK_ENTITY_TYPES.register("transporter_node_block_entity",
                    () -> BlockEntityType.Builder.of(
                            TransporterNodeBlockEntity::new,
                            DTBlocks.TRANSPORTER_NODE.get()
                    ).build(null));

    public static final RegistryObject<BlockEntityType<TransportPipeBlockEntity>> TRANSPORT_PIPE =
            BLOCK_ENTITY_TYPES.register("transport_pipe_block_entity",
                    () -> BlockEntityType.Builder.of(
                            TransportPipeBlockEntity::new,
                            DTBlocks.TRANSPORT_PIPE.get()
                    ).build(null));

    public static final RegistryObject<BlockEntityType<FluidTransporterNodeBlockEntity>> FLUID_TRANSPORTER_NODE =
            BLOCK_ENTITY_TYPES.register("fluid_transporter_node_be",
                    () -> BlockEntityType.Builder.of(
                            FluidTransporterNodeBlockEntity::new,
                            DTBlocks.FLUID_TRANSPORTER_NODE.get()
                    ).build(null));
}
