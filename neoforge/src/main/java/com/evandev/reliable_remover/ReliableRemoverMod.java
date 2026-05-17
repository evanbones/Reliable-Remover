package com.evandev.reliable_remover;

import com.evandev.reliable_remover.client.ClientSetup;
import com.evandev.reliable_remover.command.ReliableRemoverCommands;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@Mod(ReliableRemoverMod.MOD_ID)
public class ReliableRemoverMod {
    public static final String MOD_ID = "reliable_remover";

    public ReliableRemoverMod(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::commonSetup);
        NeoForge.EVENT_BUS.addListener(ReliableRemoverMod::onRegisterCommands);

        if (FMLEnvironment.dist.isClient()) {
            ClientSetup.init(modEventBus, modContainer);
        }
    }

    public static void onRegisterCommands(RegisterCommandsEvent event) {
        ReliableRemoverCommands.register(event.getDispatcher());
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        CommonClass.init();
    }
}