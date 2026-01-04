package com.evandev.reliable_remover;

import com.evandev.reliable_remover.config.RuleManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;

public class ReliableRemoverMod implements ModInitializer {

    @Override
    public void onInitialize() {
        CommonClass.init();
    }

    ServerLifecycleEvents.SERVER_STARTING.register(server -> RuleManager.load());
    ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, resourceManager, success) -> {
        if (success) RuleManager.load();
    });
}