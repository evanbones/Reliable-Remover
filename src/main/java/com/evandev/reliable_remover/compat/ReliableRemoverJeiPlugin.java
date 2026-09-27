package com.evandev.reliable_remover.compat;

import com.evandev.reliable_remover.Constants;
import com.evandev.reliable_remover.config.ModConfig;
import com.evandev.reliable_remover.config.RuleManager;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.recipe.IRecipeManager;
import mezz.jei.api.recipe.vanilla.IJeiIngredientInfoRecipe;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import java.util.List;

@JeiPlugin
public class ReliableRemoverJeiPlugin implements IModPlugin {

    @Override
    public Identifier getPluginUid() {
        return Identifier.fromNamespaceAndPath(Constants.MOD_ID, "jei_plugin");
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
        RuleManager.load();

        if (!ModConfig.get().removeItemsFromRecipeViewers) return;

        var ingredientManager = jeiRuntime.getIngredientManager();
        List<ItemStack> itemsToHide = ingredientManager.getAllIngredients(VanillaTypes.ITEM_STACK).stream()
                .filter(stack -> !stack.isEmpty() && RuleManager.isCreativeBlocked(stack))
                .toList();

        if (!itemsToHide.isEmpty()) {
            ingredientManager.removeIngredientsAtRuntime(VanillaTypes.ITEM_STACK, itemsToHide);
        }

        IRecipeManager recipeManager = jeiRuntime.getRecipeManager();
        boolean hideGlobalInfo = ModConfig.get().removeItemsFromInfoTabs;
        List<IJeiIngredientInfoRecipe> recipesToHide = recipeManager.createRecipeLookup(RecipeTypes.INFORMATION)
                .get()
                .filter(recipe -> recipe.getIngredients().stream()
                        .map(typed -> typed.getIngredient(VanillaTypes.ITEM_STACK).orElse(ItemStack.EMPTY))
                        .anyMatch(stack -> !stack.isEmpty() && ((hideGlobalInfo && RuleManager.isCreativeBlocked(stack)) || RuleManager.isInfoBlocked(stack))))
                .toList();

        if (!recipesToHide.isEmpty()) {
            recipeManager.hideRecipes(RecipeTypes.INFORMATION, recipesToHide);
        }
    }
}
