package com.evandev.reliable_remover.forge;

//? if forge {
/*import com.evandev.reliable_remover.CommonClass;
import com.evandev.reliable_remover.Constants;
import com.evandev.reliable_remover.command.ReliableRemoverCommands;
import com.evandev.reliable_remover.config.ReloadListener;
import com.evandev.reliable_remover.forge.client.ClientSetup;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;

@Mod(Constants.MOD_ID)
public class ReliableRemoverMod {

    public ReliableRemoverMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.addListener(this::commonSetup);
        MinecraftForge.EVENT_BUS.addListener(this::addReloadListener);
        MinecraftForge.EVENT_BUS.addListener(ReliableRemoverMod::onRegisterCommands);

        if (FMLEnvironment.dist.isClient()) {
            ClientSetup.init(modEventBus);
        }
    }

    public static void onRegisterCommands(RegisterCommandsEvent event) {
        ReliableRemoverCommands.register(event.getDispatcher());
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        CommonClass.init();
    }

    private void addReloadListener(final AddReloadListenerEvent event) {
        event.addListener(new ReloadListener());
    }
}
*///?}
