package com.evandev.reliable_remover.compat;

import com.evandev.reliable_remover.Constants;
import com.evandev.reliable_remover.config.ModConfig;
import com.evandev.reliable_remover.config.RuleManager;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
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

        List<ItemStack> itemsToRemove = new ArrayList<>();

        BuiltInRegistries.ITEM.forEach(item -> {
            ItemStack stack = new ItemStack(item);
            if (RuleManager.isHidden(stack)) {
                itemsToRemove.add(stack);
            }
        });

        if (!itemsToRemove.isEmpty()) {
            jeiRuntime.getIngredientManager().removeIngredientsAtRuntime(VanillaTypes.ITEM_STACK, itemsToRemove);
        }
    }
}