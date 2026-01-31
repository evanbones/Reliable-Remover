package com.evandev.reliable_remover;

import com.evandev.reliable_remover.client.ClientConfigSetup;
import com.evandev.reliable_remover.command.ReliableRemoverCommands;
import com.evandev.reliable_remover.config.ReloadListener;
import com.evandev.reliable_remover.config.RuleManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;

@Mod(ReliableRemoverMod.MOD_ID)
public class ReliableRemoverMod {
    public static final String MOD_ID = "reliable_remover";

    public ReliableRemoverMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.addListener(this::commonSetup);
        MinecraftForge.EVENT_BUS.addListener(ReliableRemoverMod::onRegisterCommands);
        MinecraftForge.EVENT_BUS.addListener(this::addReloadListener);

        if (FMLEnvironment.dist.isClient()) {
            ClientConfigSetup.register(ModLoadingContext.get().getActiveContainer());
        }
    }

    public static void onRegisterCommands(RegisterCommandsEvent event) {
        ReliableRemoverCommands.register(event.getDispatcher());
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        RuleManager.load();
    }

    private void addReloadListener(final AddReloadListenerEvent event) {
        event.addListener(new ReloadListener());
    }
}