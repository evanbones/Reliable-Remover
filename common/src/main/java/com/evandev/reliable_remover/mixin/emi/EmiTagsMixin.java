package com.evandev.reliable_remover.mixin.emi;

import com.evandev.reliable_remover.config.ModConfig;
import com.evandev.reliable_remover.config.RuleManager;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.registry.EmiTags;
import dev.emi.emi.runtime.EmiTagKey;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.stream.Collectors;

@Mixin(value = EmiTags.class, remap = false)
public class EmiTagsMixin {

    @Inject(method = "getValues", at = @At("RETURN"), cancellable = true)
    private static void reliable_remover$filterEmiTags(EmiTagKey<?> key, CallbackInfoReturnable<List<EmiStack>> cir) {
        if (!ModConfig.get().removeItemsFromEmi) return;

        List<EmiStack> original = cir.getReturnValue();
        if (original != null && !original.isEmpty()) {
            List<EmiStack> filtered = original.stream()
                    .filter(stack -> {
                        try {
                            return stack.getItemStack() == null || stack.getItemStack().isEmpty() || !RuleManager.isCreativeBlocked(stack.getItemStack());
                        } catch (Exception e) {
                            return true;
                        }
                    })
                    .collect(Collectors.toList());

            if (filtered.size() != original.size()) {
                cir.setReturnValue(filtered);
            }
        }
    }
}