package com.evandev.reliable_remover.client;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;

public class ClientSetup {

    public static void init(IEventBus modEventBus, ModContainer modContainer) {
        ClientConfigSetup.register(modContainer);
        modEventBus.addListener(ClientSetup::registerKeyBindings);
    }

    private static void registerKeyBindings(RegisterKeyMappingsEvent event) {
        event.register(Keybinds.EMI_DELETE_KEY);
    }
}