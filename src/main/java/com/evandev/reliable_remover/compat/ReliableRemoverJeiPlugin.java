package com.evandev.reliable_remover.compat;

import com.evandev.reliable_remover.Constants;
import com.evandev.reliable_remover.config.ModConfig;
import com.evandev.reliable_remover.config.RuleManager;
import com.evandev.reliable_remover.platform.Services;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.ingredient.IRecipeSlotView;
import mezz.jei.api.ingredients.IIngredientType;
import mezz.jei.api.ingredients.IIngredientTypeWithSubtypes;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.recipe.*;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.recipe.vanilla.IJeiAnvilRecipe;
import mezz.jei.api.recipe.vanilla.IJeiIngredientInfoRecipe;
import mezz.jei.api.runtime.IIngredientManager;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;

import java.util.*;

@JeiPlugin
public class ReliableRemoverJeiPlugin implements IModPlugin {
    private static final List<Runnable> RESTORE_ACTIONS = new ArrayList<>();
    private static final boolean TMRV_LOADED = Services.PLATFORM.isModLoaded("toomanyrecipeviewers"); // TMRV locks its JEI registries once plugins finish loading, and hiding is already handled by the EMI mixins
    private static IJeiRuntime runtime;
    private static IIngredientTypeWithSubtypes<Fluid, ?> fluidType;
    private static boolean syncHookRegistered;

    static void runLater(Runnable task) {
        //? if >=26.1 {
        Minecraft.getInstance().schedule(task);
        //?} else {
        /*Minecraft.getInstance().tell(task);
         *///?}
    }

    public static IJeiRuntime getRuntime() {
        return runtime;
    }

    public static void refresh() {
        if (runtime == null) return;

        for (int i = RESTORE_ACTIONS.size() - 1; i >= 0; i--) {
            try {
                RESTORE_ACTIONS.get(i).run();
            } catch (Throwable t) {
                Constants.LOG.warn("Failed to restore a JEI entry", t);
            }
        }
        RESTORE_ACTIONS.clear();
        apply();
    }

    public static void hideItem(ItemStack stack) {
        if (runtime == null || TMRV_LOADED || stack.isEmpty()) return;

        IIngredientManager ingredientManager = runtime.getIngredientManager();
        List<ItemStack> matching = ingredientManager.getAllIngredients(VanillaTypes.ITEM_STACK).stream()
                .filter(s -> s.getItem() == stack.getItem())
                .toList();
        hideIngredients(ingredientManager, VanillaTypes.ITEM_STACK, matching);
    }

    private static void apply() {
        if (TMRV_LOADED || !ModConfig.get().removeItemsFromRecipeViewers) return;

        try {
            IIngredientManager ingredientManager = runtime.getIngredientManager();
            List<ItemStack> items = ingredientManager.getAllIngredients(VanillaTypes.ITEM_STACK).stream()
                    .filter(RuleManager::isRecipeViewerBlocked)
                    .toList();
            List<?> fluids = getHiddenFluids(ingredientManager, fluidType);

            hideRecipesUsing(items, fluids);
            hideInfoRecipes();
            hideAnvilRecipes();

            hideIngredients(ingredientManager, VanillaTypes.ITEM_STACK, items);
            hideFluids(ingredientManager, fluidType, fluids);
        } catch (Throwable t) {
            Constants.LOG.error("Failed to hide removed items from JEI", t);
        }
    }

    private static <T> void hideIngredients(IIngredientManager ingredientManager, IIngredientType<T> type, List<T> ingredients) {
        if (ingredients.isEmpty()) return;
        ingredientManager.removeIngredientsAtRuntime(type, ingredients);
        RESTORE_ACTIONS.add(() -> ingredientManager.addIngredientsAtRuntime(type, ingredients));
    }

    private static <T> List<T> getHiddenFluids(IIngredientManager ingredientManager, IIngredientTypeWithSubtypes<Fluid, T> type) {
        return ingredientManager.getAllIngredients(type).stream()
                .filter(fluid -> isFluidHidden(type.getBase(fluid)))
                .toList();
    }

    @SuppressWarnings("unchecked")
    private static <T> void hideFluids(IIngredientManager ingredientManager, IIngredientTypeWithSubtypes<Fluid, T> type, List<?> fluids) {
        hideIngredients(ingredientManager, type, (List<T>) fluids);
    }

    private static boolean isFluidHidden(Fluid fluid) {
        return RuleManager.isFluidHidden(BuiltInRegistries.FLUID.getKey(fluid).toString());
    }

    /**
     * Hides every recipe with an input or output slot that only holds removed ingredients
     */
    private static void hideRecipesUsing(List<ItemStack> items, List<?> fluids) {
        if (items.isEmpty() && fluids.isEmpty()) return;

        IFocusFactory focusFactory = runtime.getJeiHelpers().getFocusFactory();
        List<IFocus<?>> focuses = new ArrayList<>();
        for (RecipeIngredientRole role : List.of(RecipeIngredientRole.INPUT, RecipeIngredientRole.OUTPUT)) {
            for (ItemStack stack : items) {
                focuses.add(focusFactory.createFocus(role, VanillaTypes.ITEM_STACK, stack));
            }
            addFluidFocuses(focuses, focusFactory, role, fluidType, fluids);
        }

        IRecipeManager recipeManager = runtime.getRecipeManager();
        IFocusGroup noFocus = focusFactory.getEmptyFocusGroup();
        recipeManager.createRecipeCategoryLookup()
                .limitFocus(focuses)
                .get()
                .toList()
                .forEach(category -> hideRecipesUsing(recipeManager, category, focuses, noFocus));
    }

    @SuppressWarnings("unchecked")
    private static <T> void addFluidFocuses(List<IFocus<?>> focuses, IFocusFactory focusFactory, RecipeIngredientRole role, IIngredientTypeWithSubtypes<Fluid, T> type, List<?> fluids) {
        for (Object fluid : fluids) {
            focuses.add(focusFactory.createFocus(role, type, (T) fluid));
        }
    }

    private static <T> void hideRecipesUsing(IRecipeManager recipeManager, IRecipeCategory<T> category, List<IFocus<?>> focuses, IFocusGroup noFocus) {
        var recipeType = category.getRecipeType();
        Set<T> candidates = new LinkedHashSet<>();
        recipeManager.createRecipeLookup(recipeType).limitFocus(focuses).get().forEach(candidates::add);

        List<T> toHide = new ArrayList<>();
        for (T recipe : candidates) {
            try {
                if (hasRemovedSlot(recipeManager, category, recipe, noFocus)) toHide.add(recipe);
            } catch (Throwable t) {
                Constants.LOG.debug("Failed to check JEI recipe in {}, leaving it visible.", category.getTitle().getString(), t);
            }
        }

        if (!toHide.isEmpty()) {
            recipeManager.hideRecipes(recipeType, toHide);
            RESTORE_ACTIONS.add(() -> recipeManager.unhideRecipes(recipeType, toHide));
        }
    }

    private static <T> boolean hasRemovedSlot(IRecipeManager recipeManager, IRecipeCategory<T> category, T recipe, IFocusGroup noFocus) {
        var layout = recipeManager.createRecipeLayoutDrawable(category, recipe, noFocus);
        if (layout.isEmpty()) return false;

        for (IRecipeSlotView slot : layout.get().getRecipeSlotsView().getSlotViews()) {
            RecipeIngredientRole role = slot.getRole();
            if (role != RecipeIngredientRole.INPUT && role != RecipeIngredientRole.OUTPUT) continue;
            if (isSlotRemoved(slot)) return true;
        }
        return false;
    }

    private static boolean isSlotRemoved(IRecipeSlotView slot) {
        List<ITypedIngredient<?>> ingredients = slot.getAllIngredients().toList();
        return !ingredients.isEmpty() && ingredients.stream().allMatch(ReliableRemoverJeiPlugin::isIngredientRemoved);
    }

    private static boolean isIngredientRemoved(ITypedIngredient<?> ingredient) {
        Optional<ItemStack> stack = ingredient.getIngredient(VanillaTypes.ITEM_STACK);
        if (stack.isPresent()) return RuleManager.isRecipeViewerBlocked(stack.get());
        return getFluid(ingredient, fluidType).map(ReliableRemoverJeiPlugin::isFluidHidden).orElse(false);
    }

    private static <T> Optional<Fluid> getFluid(ITypedIngredient<?> ingredient, IIngredientTypeWithSubtypes<Fluid, T> type) {
        return ingredient.getIngredient(type).map(type::getBase);
    }

    /**
     * Hides information pages once every item they're about is removed or has its info blocked
     */
    private static void hideInfoRecipes() {
        IRecipeManager recipeManager = runtime.getRecipeManager();
        boolean hideRemoved = ModConfig.get().removeItemsFromInfoTabs;
        List<IJeiIngredientInfoRecipe> toHide = recipeManager.createRecipeLookup(RecipeTypes.INFORMATION)
                .get()
                .filter(recipe -> {
                    List<ItemStack> stacks = recipe.getIngredients().stream()
                            .map(typed -> typed.getIngredient(VanillaTypes.ITEM_STACK).orElse(ItemStack.EMPTY))
                            .filter(stack -> !stack.isEmpty())
                            .toList();
                    return !stacks.isEmpty() && stacks.stream().allMatch(stack ->
                            (hideRemoved && RuleManager.isRecipeViewerBlocked(stack)) || RuleManager.isInfoBlocked(stack));
                })
                .toList();

        if (!toHide.isEmpty()) {
            recipeManager.hideRecipes(RecipeTypes.INFORMATION, toHide);
            RESTORE_ACTIONS.add(() -> recipeManager.unhideRecipes(RecipeTypes.INFORMATION, toHide));
        }
    }

    private static void hideAnvilRecipes() {
        IRecipeManager recipeManager = runtime.getRecipeManager();
        List<IJeiAnvilRecipe> toHide = recipeManager.createRecipeLookup(RecipeTypes.ANVIL)
                .get()
                .filter(recipe -> allRemoved(recipe.getOutputs())
                        || allRemoved(recipe.getLeftInputs())
                        || allRemoved(recipe.getRightInputs()))
                .toList();

        if (!toHide.isEmpty()) {
            recipeManager.hideRecipes(RecipeTypes.ANVIL, toHide);
            RESTORE_ACTIONS.add(() -> recipeManager.unhideRecipes(RecipeTypes.ANVIL, toHide));
        }
    }

    private static boolean allRemoved(List<ItemStack> stacks) {
        return stacks.stream().allMatch(stack -> stack.isEmpty() || RuleManager.isRecipeViewerBlocked(stack));
    }

    @Override
    public Identifier getPluginUid() {
        //? if >=1.21 {
        return Identifier.fromNamespaceAndPath(Constants.MOD_ID, "jei_plugin");
        //?} else {
        /*return new Identifier(Constants.MOD_ID, "jei_plugin");
         *///?}
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
        runtime = jeiRuntime;
        fluidType = jeiRuntime.getJeiHelpers().getPlatformFluidHelper().getFluidIngredientType();
        RESTORE_ACTIONS.clear();

        if (!syncHookRegistered) {
            syncHookRegistered = true;
            RuleManager.registerSyncedRulesChanged(() -> Minecraft.getInstance().execute(ReliableRemoverJeiPlugin::refresh));
        }

        RuleManager.load();
        runLater(() -> {
            if (runtime == jeiRuntime) apply();
        });
    }

    @Override
    public void onRuntimeUnavailable() {
        runtime = null;
        RESTORE_ACTIONS.clear();
    }
}
