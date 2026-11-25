package com.evandev.reliable_remover.mixin.emi;

//? if <=1.21.1 {
/*import com.evandev.reliable_remover.compat.EmiBlacklistHelper;
import com.moulberry.mixinconstraints.annotations.IfModLoaded;
import dev.emi.emi.registry.EmiStackList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@IfModLoaded("emi")
@Mixin(value = EmiStackList.class, remap = false)
public class EmiStackListMixin {

    @Inject(method = "reload", at = @At("HEAD"))
    private static void reliable_remover$resetRemovedStacks(CallbackInfo ci) {
        EmiBlacklistHelper.onReload();
    }
}
*///?}
