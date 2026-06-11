package com.evandev.reliable_remover.mixin.emi;

import com.evandev.reliable_remover.config.ModConfig;
import com.evandev.reliable_remover.config.RuleManager;
import com.evandev.reliable_remover.platform.Services;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.registry.EmiStackList;
import dev.emi.emi.runtime.EmiHidden;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

@Mixin(value = EmiStackList.class, remap = false)
public class EmiStackListMixin {

    @Unique
    private static Predicate<EmiStack> ADVANCEMENT_PREDICATE = null;

    @Inject(method = "reload", at = @At("RETURN"))
    private static void reliable_remover$forceRemoveItems(CallbackInfo ci) {
        if (!ModConfig.get().removeItemsFromEmi) return;

        try {
            if (EmiStackList.stacks != null) {
                List<EmiStack> mutableStacks = new ArrayList<>(EmiStackList.stacks);
                mutableStacks.removeIf(stack -> {
                    try {
                        return RuleManager.isCreativeBlockedIgnoringAdvancements(stack.getItemStack());
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

    @Inject(method = "bakeFiltered", at = @At("HEAD"))
    private static void reliable_remover$registerAdvancementFilter(CallbackInfo ci) {
        if (!ModConfig.get().removeItemsFromEmi || !RuleManager.hasAdvancementRules()) return;

        try {
            if (ADVANCEMENT_PREDICATE != null) {
                EmiHidden.pluginDisabledFilters.remove(ADVANCEMENT_PREDICATE);
            }
            Entity localPlayer = Services.PLATFORM.getLocalPlayer();
            ADVANCEMENT_PREDICATE = stack -> {
                try {
                    return RuleManager.isCreativeBlocked(stack.getItemStack(), localPlayer);
                } catch (Exception e) {
                    return false;
                }
            };
            EmiHidden.pluginDisabledFilters.add(ADVANCEMENT_PREDICATE);
        } catch (Throwable ignored) {
        }
    }
}
