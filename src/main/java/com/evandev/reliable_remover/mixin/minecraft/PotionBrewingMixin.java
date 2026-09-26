package com.evandev.reliable_remover.mixin.minecraft;

import com.moulberry.mixinconstraints.annotations.IfMinecraftVersion;
import org.spongepowered.asm.mixin.Mixin;

//? if <=26.2 {
import com.evandev.reliable_remover.config.RuleManager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionBrewing;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
//?}

@IfMinecraftVersion(maxVersion = "26.2", maxInclusive = true)
@Mixin(targets = "net.minecraft.world.item.alchemy.PotionBrewing")
public abstract class PotionBrewingMixin {

    //? if <=26.2 {
    @Shadow
    public abstract ItemStack mix(ItemStack ingredient, ItemStack source);

    @Inject(method = "hasMix", at = @At("RETURN"), cancellable = true)
    private void reliable_remover$preventHiddenBrewing(ItemStack source, ItemStack ingredient, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ()) {
            ItemStack result = this.mix(ingredient, source);

            if (result.isEmpty() || result == source || RuleManager.isHidden(result)) {
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
    //?}
}