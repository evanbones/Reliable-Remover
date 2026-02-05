package com.evandev.reliable_remover;

import com.evandev.reliable_remover.command.ReliableRemoverCommands;
import com.evandev.reliable_remover.config.ReloadListener;
import com.evandev.reliable_remover.config.RuleManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;

public class ReliableRemoverMod implements ModInitializer {

    @Override
    public void onInitialize() {
        // moved init and resource loader to ReloadableServerResourcesMixin
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            ReliableRemoverCommands.register(dispatcher);
        });
    }
}