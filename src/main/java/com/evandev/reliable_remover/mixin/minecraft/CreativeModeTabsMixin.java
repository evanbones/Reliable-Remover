package com.evandev.reliable_remover.mixin.minecraft;

import com.evandev.reliable_remover.config.RuleManager;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CreativeModeTabs.class)
public abstract class CreativeModeTabsMixin {

    @Shadow
    private static CreativeModeTab.ItemDisplayParameters CACHED_PARAMETERS;

    @Inject(method = "tryRebuildTabContents", at = @At("HEAD"))
    private static void reliable_remover$forceRebuild(FeatureFlagSet enabledFeatures, boolean hasPermissions, HolderLookup.Provider holders, CallbackInfoReturnable<Boolean> cir) {
        if (RuleManager.consumeCreativeTabsDirty()) {
            CACHED_PARAMETERS = null;
        }
    }
}
