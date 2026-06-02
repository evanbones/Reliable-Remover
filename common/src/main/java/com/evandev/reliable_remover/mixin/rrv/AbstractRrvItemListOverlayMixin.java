package com.evandev.reliable_remover.mixin.rrv;

import cc.cassian.rrv.common.overlay.itemlist.AbstractRrvItemListOverlay;
import com.evandev.reliable_remover.config.ModConfig;
import com.evandev.reliable_remover.config.RuleManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = AbstractRrvItemListOverlay.class, remap = false)
public class AbstractRrvItemListOverlayMixin {

    @Inject(method = "updateSlots", at = @At("HEAD"))
    private void reliable_remover$filterRrvItems(CallbackInfo ci) {
        if (!ModConfig.get().removeItemsFromRrv) return;

        AbstractRrvItemListOverlay overlay = (AbstractRrvItemListOverlay) (Object) this;
        if (overlay.availableItems() != null) {
            overlay.availableItems().removeIf(stack ->
                    RuleManager.isHidden(stack) || RuleManager.isCreativeBlocked(stack)
            );
        }
    }
}