package com.evandev.reliable_remover.mixin.emi;

//? if <=1.21.1 {
/*import com.evandev.reliable_remover.Constants;
import com.evandev.reliable_remover.compat.EmiBlacklistHelper;
import com.moulberry.mixinconstraints.annotations.IfModLoaded;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.runtime.EmiHidden;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@IfModLoaded("emi")
@Mixin(value = EmiHidden.class, remap = false)
public class EmiHiddenMixin {

    @Inject(method = "isHidden", at = @At("RETURN"), cancellable = true)
    private static void reliable_remover$hideRemovedStacks(EmiIngredient stack, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ()) return;

        try {
            if (EmiBlacklistHelper.isRemoved(stack)) cir.setReturnValue(true);
        } catch (Throwable t) {
            Constants.LOG.warn("Failed to check an EMI stack, leaving it visible.", t);
        }
    }
}
*///?}
