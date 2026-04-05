package com.evandev.reliable_remover.client;

import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;

public class ClientSetup {

    public static void init(IEventBus modEventBus) {
        ClientConfigSetup.register(ModLoadingContext.get().getActiveContainer());
        modEventBus.addListener(ClientSetup::registerKeyBindings);
    }

    private static void registerKeyBindings(RegisterKeyMappingsEvent event) {
        event.register(Keybinds.EMI_DELETE_KEY);
    }
}