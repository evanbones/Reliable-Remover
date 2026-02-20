package com.evandev.reliable_remover.mixin.emi;

import com.evandev.reliable_remover.config.ModConfig;
import com.evandev.reliable_remover.config.RuleManager;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.registry.EmiRegistryImpl;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = EmiRegistryImpl.class, remap = false)
public class EmiRegistryImplMixin {
    @Inject(method = "addRecipe", at = @At("HEAD"), cancellable = true)
    private void reliable_remover$filterInfoTabs(EmiRecipe recipe, CallbackInfo ci) {
        if (!ModConfig.get().removeItemsFromInfoTabs) return;

        ResourceLocation categoryId = recipe.getCategory().getId();
        if (categoryId != null && categoryId.getNamespace().equals("emi") && categoryId.getPath().equals("info")) {
            boolean shouldHide = false;
            for (EmiIngredient ingredient : recipe.getInputs()) {
                for (EmiStack emiStack : ingredient.getEmiStacks()) {
                    if (emiStack.getItemStack() != null && !emiStack.getItemStack().isEmpty() && RuleManager.isHidden(emiStack.getItemStack())) {
                        shouldHide = true;
                        break;
                    }
                }
            }
            if (!shouldHide) {
                for (EmiStack emiStack : recipe.getOutputs()) {
                    if (emiStack.getItemStack() != null && !emiStack.getItemStack().isEmpty() && RuleManager.isHidden(emiStack.getItemStack())) {
                        shouldHide = true;
                        break;
                    }
                }
            }

            if (shouldHide) ci.cancel();
        }
    }
}