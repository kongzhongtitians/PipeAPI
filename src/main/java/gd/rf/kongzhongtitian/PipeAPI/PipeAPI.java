package gd.rf.kongzhongtitian.PipeAPI;

import com.mojang.logging.LogUtils;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLConstructModEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(PipeAPI.MODID)
public class PipeAPI {

    // Define mod id in a common place for everything to reference
    public static final String MODID = "pipe_api";
    // Directly reference a slf4j logger
    private static final Logger LOGGER = LogUtils.getLogger();

    public PipeAPI() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::onConstructMod);

        DTBlocks.BLOCKS.register(modEventBus);
        DTItems.ITEMS.register(modEventBus);
        DTBlockEntity.BLOCK_ENTITY_TYPES.register(modEventBus);
        DTMenu.MENUS.register(modEventBus);

        LOGGER.info("PipeAPI Has Loaded");
    }

    private void onConstructMod(final FMLConstructModEvent event) {
    }
}
