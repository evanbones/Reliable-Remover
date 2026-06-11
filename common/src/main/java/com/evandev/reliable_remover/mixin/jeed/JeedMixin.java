package com.evandev.reliable_remover.mixin.jeed;

import com.evandev.reliable_remover.config.ModConfig;
import net.mehvahdjukaar.jeed.Jeed;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffect;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

@Mixin(value = Jeed.class, remap = false)
public class JeedMixin {

    /**
     * Intercepts JEED's effect list generation and filters out any
     * effects whose IDs are present in Reliable Remover's blacklist.
     */
    @Inject(method = "getEffectList", at = @At("RETURN"), cancellable = true)
    private static void reliable_remover$filterJeedEffects(CallbackInfoReturnable<List<MobEffect>> cir) {
        List<MobEffect> originalList = cir.getReturnValue();

        if (originalList != null && !originalList.isEmpty()) {
            List<MobEffect> filteredList = new ArrayList<>(originalList);

            filteredList.removeIf(effect -> {
                var id = BuiltInRegistries.MOB_EFFECT.getKey(effect);
                return id != null && ModConfig.get().blacklistedItems.contains(id.toString());
            });

            if (filteredList.size() != originalList.size()) {
                cir.setReturnValue(filteredList);
            }
        }
    }
}