package com.evandev.reliable_remover.forge.client;

//? if forge {
/*import com.evandev.reliable_remover.client.Keybinds;
import com.evandev.reliable_remover.config.YaclConfigIntegration;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;

public class ClientSetup {

    public static void init(IEventBus modEventBus) {
        if (ModList.get().isLoaded("yet_another_config_lib_v3")) {
            ModLoadingContext.get().registerExtensionPoint(
                    ConfigScreenHandler.ConfigScreenFactory.class,
                    () -> new ConfigScreenHandler.ConfigScreenFactory((client, parent) -> YaclConfigIntegration.createScreen(parent))
            );
        }
        modEventBus.addListener(ClientSetup::registerKeyBindings);
    }

    private static void registerKeyBindings(RegisterKeyMappingsEvent event) {
        event.register(Keybinds.EMI_DELETE_KEY);
    }
}
*///?}
