package com.evandev.reliable_remover.mixin.minecraft;

import com.evandev.reliable_remover.config.RuleManager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionBrewing;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PotionBrewing.class)
public class PotionBrewingMixin {

    @Inject(method = "hasMix", at = @At("RETURN"), cancellable = true)
    private static void reliable_remover$preventHiddenBrewing(ItemStack input, ItemStack reagent, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ()) {
            ItemStack result = PotionBrewing.mix(reagent, input);

            if (result.isEmpty() || result == input || RuleManager.isHidden(result)) {
                cir.setReturnValue(false);
            }
        }
    }

    @Inject(method = "mix", at = @At("RETURN"), cancellable = true)
    private static void reliable_remover$filterMixedPotion(ItemStack reagent, ItemStack p_potion, CallbackInfoReturnable<ItemStack> cir) {
        ItemStack result = cir.getReturnValue();
        if (!result.isEmpty() && RuleManager.isHidden(result)) {
            cir.setReturnValue(p_potion);
        }
    }
}