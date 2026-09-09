package com.evandev.reliable_remover.mixin.emi;

import com.evandev.reliable_remover.Constants;
import com.evandev.reliable_remover.compat.EmiBlacklistHelper;
import com.evandev.reliable_remover.config.ModConfig;
import com.evandev.reliable_remover.config.RuleManager;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.registry.EmiRegistryImpl;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = EmiRegistryImpl.class, remap = false)
public class EmiRegistryImplMixin {

    @Unique
    private static boolean reliable_remover$shouldHideRecipe(EmiRecipe recipe) {
        ResourceLocation categoryId = recipe.getCategory().getId();
        boolean isInfoTab = categoryId != null && categoryId.getNamespace().equals("emi") && categoryId.getPath().equals("info");

        if (!recipe.supportsRecipeTree()) {
            return reliable_remover$allSubjectsRemoved(recipe, isInfoTab);
        }

        for (EmiStack emiStack : recipe.getOutputs()) {
            if (reliable_remover$isOutputRemoved(emiStack, isInfoTab)) return true;
        }

        for (EmiIngredient ingredient : recipe.getInputs()) {
            if (ingredient != EmiStack.EMPTY && ingredient.isEmpty()) return true;

            for (EmiStack emiStack : ingredient.getEmiStacks()) {
                if (EmiBlacklistHelper.isEmiStackBlacklisted(emiStack)) return true;
                if (isInfoTab && reliable_remover$isInfoSubjectRemoved(emiStack)) return true;
            }
        }

        return false;
    }

    /**
     * True when every stack an informational recipe is about has been removed.
     */
    @Unique
    private static boolean reliable_remover$allSubjectsRemoved(EmiRecipe recipe, boolean isInfoTab) {
        boolean sawSubject = false;

        for (EmiStack emiStack : recipe.getOutputs()) {
            if (emiStack == EmiStack.EMPTY || emiStack.isEmpty()) continue;
            sawSubject = true;
            if (!reliable_remover$isSubjectRemoved(emiStack, isInfoTab)) return false;
        }

        if (sawSubject) return true;

        for (EmiIngredient ingredient : recipe.getInputs()) {
            if (ingredient == EmiStack.EMPTY) continue;
            for (EmiStack emiStack : ingredient.getEmiStacks()) {
                if (emiStack == EmiStack.EMPTY || emiStack.isEmpty()) continue;
                sawSubject = true;
                if (!reliable_remover$isSubjectRemoved(emiStack, isInfoTab)) return false;
            }
        }

        return sawSubject;
    }

    @Unique
    private static boolean reliable_remover$isSubjectRemoved(EmiStack emiStack, boolean isInfoTab) {
        return isInfoTab
                ? reliable_remover$isInfoSubjectRemoved(emiStack)
                : reliable_remover$isOutputRemoved(emiStack, false);
    }

    @Unique
    private static boolean reliable_remover$isOutputRemoved(EmiStack emiStack, boolean isInfoTab) {
        if (EmiBlacklistHelper.isEmiStackBlacklisted(emiStack)) return true;

        ItemStack stack = emiStack.getItemStack();
        if (stack == null || stack.isEmpty()) return false;

        return RuleManager.isCreativeBlocked(stack)
                || (isInfoTab && RuleManager.isInfoBlocked(stack));
    }

    @Unique
    private static boolean reliable_remover$isInfoSubjectRemoved(EmiStack emiStack) {
        if (EmiBlacklistHelper.isEmiStackBlacklisted(emiStack)) return true;

        ItemStack stack = emiStack.getItemStack();
        if (stack == null || stack.isEmpty()) return false;

        return (ModConfig.get().removeItemsFromInfoTabs && RuleManager.isCreativeBlocked(stack))
                || RuleManager.isInfoBlocked(stack);
    }

    @Inject(method = "addEmiStack", at = @At("HEAD"), cancellable = true)
    private void reliable_remover$filterAddedEmiStacks(EmiStack stack, CallbackInfo ci) {
        if (!ModConfig.get().removeItemsFromEmi) return;

        try {
            if (EmiBlacklistHelper.isEmiStackBlacklisted(stack)) {
                ci.cancel();
            } else if (stack.getItemStack() != null && !stack.getItemStack().isEmpty() && RuleManager.isCreativeBlocked(stack.getItemStack())) {
                ci.cancel();
            }
        } catch (Throwable t) {
            Constants.LOG.warn("Failed to filter an EMI stack, leaving it visible.", t);
        }
    }

    @Inject(method = "addRecipe", at = @At("HEAD"), cancellable = true)
    private void reliable_remover$filterEmiRecipes(EmiRecipe recipe, CallbackInfo ci) {
        if (!ModConfig.get().removeItemsFromEmi) return;

        try {
            if (reliable_remover$shouldHideRecipe(recipe)) ci.cancel();
        } catch (Throwable t) {
            Constants.LOG.warn("Failed to filter EMI recipe {}, leaving it visible.", recipe.getId(), t);
        }
    }
}