package com.evandev.reliable_remover.neoforge.client;

//? if neoforge {
/*import com.evandev.reliable_remover.config.YaclConfigIntegration;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

public class ClientConfigSetup {
    public static void register(ModContainer container) {
        if (!ModList.get().isLoaded("yet_another_config_lib_v3")) return;
        container.registerExtensionPoint(IConfigScreenFactory.class, (c, parent) -> YaclConfigIntegration.createScreen(parent));
    }
}
*///?}
