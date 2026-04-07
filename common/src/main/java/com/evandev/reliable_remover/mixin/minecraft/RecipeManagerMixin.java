package com.evandev.reliable_remover.mixin.minecraft;

import com.evandev.reliable_remover.config.RuleManager;
import net.minecraft.world.Container;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.stream.Collectors;

@Mixin(RecipeManager.class)
public class RecipeManagerMixin {

    @Inject(method = "getAllRecipesFor", at = @At("RETURN"), cancellable = true)
    private <C extends Container, T extends Recipe<C>> void reliable_remover$filterRecipes(
            RecipeType<T> type, CallbackInfoReturnable<List<T>> cir) {

        List<T> recipes = cir.getReturnValue();
        if (recipes != null && !recipes.isEmpty()) {

            List<T> filtered = recipes.stream()
                    .filter(recipe -> !RuleManager.isRecipeBlocked(recipe))
                    .collect(Collectors.toList());

            cir.setReturnValue(filtered);
        }
    }
}