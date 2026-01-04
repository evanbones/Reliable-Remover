package com.evandev.reliable_remover.config;

import com.evandev.reliable_remover.Constants;
import com.evandev.reliable_remover.mixin.minecraft.CreativeModeTabAccessor;
import com.evandev.reliable_remover.platform.Services;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public class ReloadListener implements ResourceManagerReloadListener {
    @Override
    public void onResourceManagerReload(@NotNull ResourceManager resourceManager) {
        RuleManager.load();

        if (Services.PLATFORM.isPhysicalClient()) {
            for (CreativeModeTab tab : BuiltInRegistries.CREATIVE_MODE_TAB) {
                CreativeModeTabAccessor accessor = (CreativeModeTabAccessor) tab;
                if (accessor.getDisplayItems() != null) {
                    accessor.getDisplayItems().clear();
                }
                if (accessor.getDisplayItemsSearchTab() != null) {
                    accessor.getDisplayItemsSearchTab().clear();
                }
            }

            try {
                for (Field field : CreativeModeTabs.class.getDeclaredFields()) {
                    if (Modifier.isStatic(field.getModifiers()) && field.getType().getName().contains("Parameters")) {
                        field.setAccessible(true);
                        field.set(null, null);
                        break;
                    }
                }
            } catch (Exception e) {
                Constants.LOG.error("Failed to reset Creative Mode Tab cache", e);
            }
        }
    }
}