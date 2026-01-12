package com.evandev.reliable_remover;

import com.evandev.reliable_remover.client.ClientConfigSetup;
import com.evandev.reliable_remover.compat.ReliableRemoverRrvPlugin;
import com.evandev.reliable_remover.config.ReloadListener;
import com.evandev.reliable_remover.config.RuleManager;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;

@Mod(ReliableRemoverMod.MOD_ID)
public class ReliableRemoverMod {
    public static final String MOD_ID = "reliable_remover";

    public ReliableRemoverMod(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::commonSetup);
        NeoForge.EVENT_BUS.addListener(this::addReloadListener);

        if (FMLEnvironment.getDist().isClient()) {
            ClientConfigSetup.register(modContainer);
        }
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        RuleManager.load();
        if (ModList.get().isLoaded("rrv")) {
            ReliableRemoverRrvPlugin.init();
        }
    }

    private void addReloadListener(final AddServerReloadListenersEvent event) {
        event.addListener(Identifier.fromNamespaceAndPath(MOD_ID, "reload"), new ReloadListener());
    }
}