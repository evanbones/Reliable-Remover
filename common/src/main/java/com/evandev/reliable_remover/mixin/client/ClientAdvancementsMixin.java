package com.evandev.reliable_remover.mixin.client;

import com.evandev.reliable_remover.config.AdvancementCache;
import com.evandev.reliable_remover.config.RuleManager;
import com.evandev.reliable_remover.platform.Services;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.client.multiplayer.ClientAdvancements;
import net.minecraft.network.protocol.game.ClientboundUpdateAdvancementsPacket;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

@Mixin(ClientAdvancements.class)
public class ClientAdvancementsMixin {

    @Inject(method = "update", at = @At("TAIL"))
    private void reliable_remover$onAdvancementsUpdate(ClientboundUpdateAdvancementsPacket packet, CallbackInfo ci) {
        if (!RuleManager.hasAdvancementRules()) return;

        if (packet.shouldReset()) {
            AdvancementCache.clear();
            for (Map.Entry<ResourceLocation, AdvancementProgress> entry : packet.getProgress().entrySet()) {
                if (entry.getValue().isDone()) {
                    AdvancementCache.markCompleted(entry.getKey());
                }
            }
            remover$triggerEmiReloadIfLoaded();
            return;
        }

        boolean changed = false;

        for (ResourceLocation id : packet.getRemoved()) {
            if (AdvancementCache.isDone(id)) {
                changed = true;
            }
            AdvancementCache.markNotCompleted(id);
        }

        for (Map.Entry<ResourceLocation, AdvancementProgress> entry : packet.getProgress().entrySet()) {
            if (entry.getValue().isDone()) {
                if (!AdvancementCache.isDone(entry.getKey())) {
                    changed = true;
                }
                AdvancementCache.markCompleted(entry.getKey());
            } else {
                if (AdvancementCache.isDone(entry.getKey())) {
                    changed = true;
                }
                AdvancementCache.markNotCompleted(entry.getKey());
            }
        }

        if (changed) {
            remover$triggerEmiReloadIfLoaded();
        }
    }

    @Unique
    private static void remover$triggerEmiReloadIfLoaded() {
        if (Services.PLATFORM.isModLoaded("emi")) {
            com.evandev.reliable_remover.compat.EmiFastRefresh.refresh();
        }
    }
}