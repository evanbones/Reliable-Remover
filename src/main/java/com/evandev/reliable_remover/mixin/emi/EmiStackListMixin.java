package com.evandev.reliable_remover.mixin.emi;

//? if <=1.21.1 {
/*import com.evandev.reliable_remover.compat.EmiBlacklistHelper;
import com.evandev.reliable_remover.config.ModConfig;
import com.evandev.reliable_remover.config.RuleManager;
import com.moulberry.mixinconstraints.annotations.IfMinecraftVersion;
import com.moulberry.mixinconstraints.annotations.IfModLoaded;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.registry.EmiStackList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

@IfMinecraftVersion(maxVersion = "1.21.1", maxInclusive = true)
@IfModLoaded("emi")
@Mixin(value = EmiStackList.class, remap = false)
public class EmiStackListMixin {

    @Inject(method = "reload", at = @At("RETURN"))
    private static void reliable_remover$forceRemoveItems(CallbackInfo ci) {
        if (!ModConfig.get().removeItemsFromEmi) return;

        try {
            if (EmiStackList.stacks != null) {
                List<EmiStack> mutableStacks = new ArrayList<>(EmiStackList.stacks);
                mutableStacks.removeIf(stack -> {
                    if (EmiBlacklistHelper.isEmiStackBlacklisted(stack)) return true;
                    try {
                        return RuleManager.isCreativeBlocked(stack.getItemStack());
                    } catch (Exception e) {
                        return false;
                    }
                });
                EmiStackList.stacks = mutableStacks;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
*///?} else {
import com.moulberry.mixinconstraints.annotations.IfMinecraftVersion;
import com.moulberry.mixinconstraints.annotations.IfModLoaded;
import org.spongepowered.asm.mixin.Mixin;

@IfMinecraftVersion(maxVersion = "1.21.1")
@IfModLoaded("emi")
@Mixin(targets = "dev.emi.emi.registry.EmiStackList", remap = false)
public class EmiStackListMixin {
}
//?}
