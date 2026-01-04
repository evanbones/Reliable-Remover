package com.evandev.reliable_remover.client;

import com.evandev.reliable_remover.config.ClothConfigIntegration;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.fml.ModContainer;

public class ClientConfigSetup {
    public static void register(ModContainer container) {
        container.registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class, () -> new ConfigScreenHandler.ConfigScreenFactory((mc, parent) -> ClothConfigIntegration.createScreen(parent)));
    }
}