package com.evandev.reliable_remover;

import com.evandev.reliable_remover.config.ClothConfigIntegration;
import com.evandev.reliable_remover.config.RuleManager;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLConstructModEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import org.jetbrains.annotations.NotNull;

@Mod(Constants.MOD_ID)
@EventBusSubscriber(modid = Constants.MOD_ID)
public class ReliableRemoverMod {

    public ReliableRemoverMod(IEventBus eventBus) {
        CommonClass.init();

        if (ModList.get().isLoaded("cloth_config")) {
            eventBus.register(new Object() {
                @SubscribeEvent
                public void onConstructMod(FMLConstructModEvent event) {
                    ModLoadingContext.get().registerExtensionPoint(
                            IConfigScreenFactory.class,
                            () -> new IConfigScreenFactory() {
                                @Override
                                public @NotNull Screen createScreen(@NotNull ModContainer modContainer, @NotNull Screen parent) {
                                    return ClothConfigIntegration.createScreen(parent);
                                }
                            }
                    );
                }
            });
        }

        NeoForge.EVENT_BUS.addListener(this::onServerStarting);
        NeoForge.EVENT_BUS.addListener(this::onAddReloadListeners);

    }

    private void onServerStarting(ServerStartingEvent event) {
        RuleManager.load();
    }

    private void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new SimplePreparableReloadListener<Void>() {
            @Override
            protected @NotNull Void prepare(@NotNull ResourceManager pResourceManager, @NotNull ProfilerFiller pProfiler) {
                return null;
            }

            @Override
            protected void apply(@NotNull Void pObject, @NotNull ResourceManager pResourceManager, @NotNull ProfilerFiller pProfiler) {
                RuleManager.load();
            }
        });
    }

}