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
    public abstract ItemStack mix(ItemStack potion, ItemStack potionItem);

    @Inject(method = "hasMix", at = @At("RETURN"), cancellable = true)
    private void reliable_remover$preventHiddenBrewing(ItemStack potion, ItemStack ingredient, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ()) {
            ItemStack result = this.mix(potion, ingredient);
            if (result.isEmpty() || result == potion || RuleManager.isHidden(result)) {
                cir.setReturnValue(false);
            }
        }
    }

    @Inject(method = "mix", at = @At("RETURN"), cancellable = true)
    private void reliable_remover$filterMixedPotion(ItemStack potion, ItemStack ingredient, CallbackInfoReturnable<ItemStack> cir) {
        ItemStack result = cir.getReturnValue();
        if (!result.isEmpty() && RuleManager.isHidden(result)) {
            cir.setReturnValue(potion);
        }
    }
}