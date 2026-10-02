package gd.rf.kongzhongtitian.PipeAPI;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Supplier;

public class DTBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, PipeAPI.MODID);

    //深板岩矿石
    private static final BlockBehaviour.Properties METAL_DEEPSLATE_ORE_PROPERTIES =
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .sound(SoundType.METAL)
                    .strength(5.0F, 6.0F)
                    .requiresCorrectToolForDrops();

    // 修复 G：虚空漏斗补充强度与材质（此前空属性，默认 0.5 硬度空手秒挖）
    public static final RegistryObject<Block> VOID_HOPPER = registerBlock("void_hopper",
            () -> new VoidHopper(BlockBehaviour.Properties.of()
                    .strength(2.0F)
                    .sound(SoundType.METAL)));

    public static final RegistryObject<Block> TRANSPORTER_NODE = registerBlock("transporter_node",
            TransporterNode::new);

    public static final RegistryObject<Block> TRANSPORT_PIPE = registerBlock("transport_pipe",
            TransportPipe::new);

    public static final RegistryObject<Block> FLUID_TRANSPORTER_NODE = registerBlock("fluid_transporter_node", FluidTransporterNode::new);

    public static RegistryObject<Block> registerSimpleBlock(String name, BlockBehaviour.Properties properties) {
        RegistryObject<Block> block = BLOCKS.register(name, () -> new Block(properties));
        DTItems.ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
        return block;
    }

    public static RegistryObject<Block> registerSimpleBlock(String name, BlockBehaviour.Properties properties, Item.Properties itemProperties) {
        RegistryObject<Block> block = BLOCKS.register(name, () -> new Block(properties));
        DTItems.ITEMS.register(name, () -> new BlockItem(block.get(), itemProperties));
        return block;
    }

    public static RegistryObject<Block> registerBlock(String name, Supplier<Block> blockSupplier) {
        RegistryObject<Block> block = BLOCKS.register(name, blockSupplier);
        DTItems.ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
        return block;
    }
}