package com.evandev.reliable_remover;

import com.evandev.reliable_remover.config.ReloadListener;
import com.evandev.reliable_remover.config.RuleManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;

public class ReliableRemoverMod implements ModInitializer {

    @Override
    public void onInitialize() {
        RuleManager.load();

        ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(new FabricReloadListener());
    }

    private static class FabricReloadListener extends ReloadListener implements IdentifiableResourceReloadListener {
        @Override
        public ResourceLocation getFabricId() {
            return new ResourceLocation("reliable_remover", "reload_listener");
        }
    }
}