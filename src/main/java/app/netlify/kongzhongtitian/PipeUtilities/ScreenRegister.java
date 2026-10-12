package app.netlify.kongzhongtitian.PipeUtilities;

import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = PipeAPI.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ScreenRegister {
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            MenuScreens.register(DTMenu.TRANSPORTER_NODE_MENU.get(), TransporterNodeScreen::new);
            MenuScreens.register(DTMenu.FILTER_MENU.get(), FilterScreen::new);
            MenuScreens.register(DTMenu.FLUID_TRANSPORTER_NODE_MENU.get(), FluidTransporterNodeScreen::new);
        });
    }
}
