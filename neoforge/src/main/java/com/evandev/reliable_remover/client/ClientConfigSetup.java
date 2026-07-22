package com.evandev.reliable_remover.client;

import com.evandev.reliable_remover.client.ModConfigScreen;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

public class ClientConfigSetup {
    public static void register(ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, (c, parent) -> ModConfigScreen.createScreen(parent));
    }
}