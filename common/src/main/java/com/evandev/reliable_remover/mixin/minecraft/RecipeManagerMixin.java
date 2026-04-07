package com.evandev.reliable_remover.mixin.minecraft;

import com.evandev.reliable_remover.config.RuleManager;
import net.minecraft.world.item.crafting.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.stream.Collectors;

@Mixin(RecipeManager.class)
public class RecipeManagerMixin {

    @Inject(method = "getAllRecipesFor", at = @At("RETURN"), cancellable = true)
    private <I extends RecipeInput, T extends Recipe<I>> void reliable_remover$filterRecipes(
            RecipeType<T> type, CallbackInfoReturnable<List<RecipeHolder<T>>> cir) {

        List<RecipeHolder<T>> recipes = cir.getReturnValue();
        if (recipes != null && !recipes.isEmpty()) {

            List<RecipeHolder<T>> filtered = recipes.stream()
                    .filter(holder -> !RuleManager.isRecipeBlocked(holder.value()))
                    .collect(Collectors.toList());

            cir.setReturnValue(filtered);
        }
    }
}