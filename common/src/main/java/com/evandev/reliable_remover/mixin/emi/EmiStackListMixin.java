package com.evandev.reliable_remover.mixin.emi;

import com.evandev.reliable_remover.config.ModConfig;
import com.evandev.reliable_remover.config.RuleManager;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.registry.EmiStackList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

@Mixin(value = EmiStackList.class, remap = false)
public class EmiStackListMixin {

    @Inject(method = "reload", at = @At("RETURN"))
    private static void reliable_remover$forceRemoveItems(CallbackInfo ci) {
        if (!ModConfig.get().removeItemsFromEmi) return;

        try {
            if (EmiStackList.stacks != null) {
                List<EmiStack> mutableStacks = new ArrayList<>(EmiStackList.stacks);
                mutableStacks.removeIf(stack -> {
                    try {
                        return RuleManager.isHidden(stack.getItemStack());
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