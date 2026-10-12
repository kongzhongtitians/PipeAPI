package app.netlify.kongzhongtitian.PipeUtilities;

import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class DTItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, PipeAPI.MODID);

    public static final RegistryObject<Item> NODE_UPGRADE_ITEM_FILTER =
            ITEMS.register("node_upgrade_item_filter", () -> new NodeUpgradeItemFilter(new Item.Properties()));

    public static RegistryObject<Item> registerSimpleItem(String itemName){
        return ITEMS.register(itemName , ()-> new Item(new Item.Properties()));
    }
}
