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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@JeiPlugin
public class ReliableRemoverJeiPlugin implements IModPlugin {

    @Override
    public @NotNull ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "jei_plugin");
    }

    @Override
    public void onRuntimeAvailable(@NotNull IJeiRuntime jeiRuntime) {
        ModConfig.get();
        RuleManager.load();

        if (!ModConfig.get().removeItemsFromEmi) return;

        var ingredientManager = jeiRuntime.getIngredientManager();
        List<ItemStack> allStacks = ingredientManager.getAllIngredients(VanillaTypes.ITEM_STACK).stream().toList();

        List<ItemStack> itemsToHide = allStacks.stream()
                .filter(stack -> !stack.isEmpty() && RuleManager.isHidden(stack))
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
                        .anyMatch(stack -> !stack.isEmpty() && ((hideGlobalInfo && RuleManager.isHidden(stack)) || RuleManager.isInfoBlocked(stack))))
                .toList();

        if (!recipesToHide.isEmpty()) {
            recipeManager.hideRecipes(RecipeTypes.INFORMATION, recipesToHide);
        }
    }
}