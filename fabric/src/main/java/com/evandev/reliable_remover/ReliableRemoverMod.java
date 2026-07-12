package com.evandev.reliable_remover;

import com.evandev.reliable_remover.command.ReliableRemoverCommands;
import com.evandev.reliable_remover.config.ReloadListener;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;

public class ReliableRemoverMod implements ModInitializer {

    @Override
    public void onInitialize() {
        CommonClass.init();

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            ReliableRemoverCommands.register(dispatcher);
        });

        ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(new FabricReloadListener());
    }

    private static class FabricReloadListener extends ReloadListener implements IdentifiableResourceReloadListener {
        @Override
        public ResourceLocation getFabricId() {
            return ResourceLocation.fromNamespaceAndPath("reliable_remover", "reload_listener");
        }
    }
}