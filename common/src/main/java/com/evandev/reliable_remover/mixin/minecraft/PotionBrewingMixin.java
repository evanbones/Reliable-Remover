package com.evandev.reliable_remover.mixin.minecraft;

import com.evandev.reliable_remover.config.RuleManager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionBrewing;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PotionBrewing.class)
public abstract class PotionBrewingMixin {

    @Shadow
    public abstract ItemStack mix(ItemStack ingredient, ItemStack source);

    @Inject(method = "hasMix", at = @At("RETURN"), cancellable = true)
    private void reliable_remover$preventHiddenBrewing(ItemStack source, ItemStack ingredient, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ()) {
            ItemStack result = this.mix(source, ingredient);

            if (result.isEmpty() || result == ingredient || RuleManager.isHidden(result)) {
                cir.setReturnValue(false);
            }
        }
    }

    @Inject(method = "mix", at = @At("RETURN"), cancellable = true)
    private void reliable_remover$filterMixedPotion(ItemStack ingredient, ItemStack source, CallbackInfoReturnable<ItemStack> cir) {
        ItemStack result = cir.getReturnValue();
        if (!result.isEmpty() && RuleManager.isHidden(result)) {
            cir.setReturnValue(source);
        }
    }
}