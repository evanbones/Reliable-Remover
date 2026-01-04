package com.evandev.reliable_remover;

import com.evandev.reliable_remover.config.ReloadListener;
import com.evandev.reliable_remover.config.RuleManager;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddReloadListenerEvent;

@Mod(ReliableRemoverMod.MOD_ID)
public class ReliableRemoverMod {
    public static final String MOD_ID = "reliable_remover";

    public ReliableRemoverMod(IEventBus modEventBus) {
        modEventBus.addListener(this::commonSetup);
        NeoForge.EVENT_BUS.addListener(this::addReloadListener);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        RuleManager.load();
    }

    private void addReloadListener(AddReloadListenerEvent event) {
        event.addListener(new ReloadListener());
    }
}