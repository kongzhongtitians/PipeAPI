package gd.rf.kongzhongtitian.PipeAPI;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.items.ItemStackHandler;

import static net.minecraftforge.registries.ForgeRegistries.MENU_TYPES;


public class DTMenu {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(MENU_TYPES, PipeAPI.MODID);

    public static final RegistryObject<MenuType<TransporterNodeMenu>> TRANSPORTER_NODE_MENU =
            MENUS.register("transporter_node_menu", () -> IForgeMenuType.create((windowId, playerInv, extraData) -> {
                BlockPos pos = extraData.readBlockPos();
                ContainerLevelAccess access = ContainerLevelAccess.create(playerInv.player.level(), pos);

                BlockEntity blockEntity = playerInv.player.level().getBlockEntity(pos);

                // 如果方块实体类型正确，直接传入
                if (blockEntity instanceof TransporterNodeBlockEntity node) {
                    return new TransporterNodeMenu(
                            windowId,
                            playerInv,
                            node,               // ← blockEntity 参数
                            node.getItemHandler(),
                            access
                    );
                }

                // 兜底：类型不对时，用一个临时 handler 和 null blockEntity
                IItemHandler fallbackHandler =
                        new ItemStackHandler(TransporterNodeBlockEntity.INVENTORY_SIZE);
                return new TransporterNodeMenu(
                        windowId,
                        playerInv,
                        null,                   // blockEntity 为 null
                        fallbackHandler,
                        access
                );
            }));
    //public static final RegistryObject<MenuType<TransporterNodeMenu>> TRANSPORTER_NODE_MENU =
    //        MENUS.register("transporter_node_menu", () -> IForgeMenuType.create((windowId, playerInv, extraData) -> {
    //            // 从同步数据包中读取方块坐标
    //            BlockPos pos = extraData.readBlockPos();
    //            // 创建 ContainerLevelAccess
    //            ContainerLevelAccess access = ContainerLevelAccess.create(playerInv.player.level(), pos);

    //            // 从目标方块实体中获取 IItemHandler 能力
    //            BlockEntity blockEntity = playerInv.player.level().getBlockEntity(pos);
    //            IItemHandler itemHandler = blockEntity != null
    //                    ? blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER)
    //                    .orElseThrow(() -> new IllegalStateException("Expected item handler capability"))
    //                    : null;  // 安全起见可以进一步处理 null 情况

    //            return new TransporterNodeMenu(windowId, playerInv, itemHandler, access);
    //        }));

    public static final RegistryObject<MenuType<FilterMenu>> FILTER_MENU =
            MENUS.register("filter_menu", () -> IForgeMenuType.create((windowId, inv, data) -> {
                InteractionHand hand = data.readEnum(InteractionHand.class);
                ItemStack stack = inv.player.getItemInHand(hand);
                return new FilterMenu(windowId, inv, stack);
            }));

    public static final RegistryObject<MenuType<FluidTransporterNodeMenu>> FLUID_TRANSPORTER_NODE_MENU =
            MENUS.register("fluid_transporter_node_menu", () -> IForgeMenuType.create((windowId, playerInv, extraData) -> {
                BlockPos pos = extraData.readBlockPos();
                ContainerLevelAccess access = ContainerLevelAccess.create(playerInv.player.level(), pos);
                BlockEntity blockEntity = playerInv.player.level().getBlockEntity(pos);
                FluidTransporterNodeBlockEntity fluidBe =
                        blockEntity instanceof FluidTransporterNodeBlockEntity be ? be : null;
                return new FluidTransporterNodeMenu(windowId, playerInv, fluidBe, access);
            }));
}
