package com.evandev.reliable_remover.mixin.minecraft;

import com.evandev.reliable_remover.config.RuleManager;
import net.minecraft.core.RegistryAccess;
import net.minecraft.server.ReloadableServerResources;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ReloadableServerResources.class, priority = 1100)
public class ReloadableServerResourcesMixin {
    @Inject(
            method = "updateRegistryTags(Lnet/minecraft/core/RegistryAccess;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/Blocks;rebuildCache()V"
            )
    )
    private void reliableRemover$onTagsLoaded(RegistryAccess registryAccess, CallbackInfo ci) {
        RuleManager.expandTagRules(registryAccess);
    }
}
