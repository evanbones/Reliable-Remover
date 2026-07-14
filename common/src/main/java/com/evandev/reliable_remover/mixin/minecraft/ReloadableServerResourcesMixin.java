package com.evandev.reliable_remover.mixin.minecraft;

import com.evandev.reliable_remover.config.RuleManager;
import net.minecraft.server.ReloadableServerRegistries;
import net.minecraft.server.ReloadableServerResources;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ReloadableServerResources.class, priority = 1100)
public abstract class ReloadableServerResourcesMixin {

    @Shadow
    public abstract ReloadableServerRegistries.Holder fullRegistries();

    @Inject(
            method = "updateRegistryTags()V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/Blocks;rebuildCache()V"
            )
    )
    private void reliableRemover$onTagsLoaded(CallbackInfo ci) {
        RuleManager.expandTagRules(this.fullRegistries().get());
    }
}
