package com.evandev.reliable_remover.mixin.jeed;

import com.evandev.reliable_remover.config.RuleManager;
import com.moulberry.mixinconstraints.annotations.IfModLoaded;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

@IfModLoaded("jeed")
@Mixin(targets = "net.mehvahdjukaar.jeed.common.EffectInfo", remap = false)
public class JeedEffectInfoMixin {

    @Inject(method = "computeItemProviders", at = @At("RETURN"), cancellable = true, require = 0)
    private static void reliable_remover$filterItemProviders(CallbackInfoReturnable<List<ItemStack>> cir) {
        List<ItemStack> providers = cir.getReturnValue();
        if (providers == null || providers.isEmpty()) return;
        List<ItemStack> filtered = new ArrayList<>(providers);
        if (filtered.removeIf(RuleManager::isRecipeViewerBlocked)) {
            cir.setReturnValue(filtered);
        }
    }
}
