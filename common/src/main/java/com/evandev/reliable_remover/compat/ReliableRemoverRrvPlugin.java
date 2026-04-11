package com.evandev.reliable_remover.compat;

import cc.cassian.rrv.api.ReliableRecipeViewerPlugin;
import cc.cassian.rrv.api.recipe.ItemView;
import cc.cassian.rrv.common.recipe.ItemViewRecipes;
import cc.cassian.rrv.common.recipe.inventory.SlotContent;
import com.evandev.reliable_remover.config.ModConfig;
import com.evandev.reliable_remover.config.RuleManager;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;

public class ReliableRemoverRrvPlugin implements ReliableRecipeViewerPlugin {

    public static void init() {
        ItemView.addClientReloadCallback(() -> {
            ModConfig.get();
            RuleManager.load();

            if (!ModConfig.get().removeItemsFromRrv) return;

            BuiltInRegistries.ITEM.forEach(item -> {
                if (RuleManager.isHidden(item.getDefaultInstance())) {
                    ItemView.excludeItem(item);
                }
            });

            ItemViewRecipes.INFO_RECIPES.removeIf(recipe -> {
                for (SlotContent ingredient : recipe.getIngredients()) {
                    for (ItemStack stack : ingredient.getValidContents()) {
                        if (RuleManager.isInfoBlocked(stack) || RuleManager.isHidden(stack)) {
                            return true;
                        }
                    }
                }
                return false;
            });
        });
    }

    @Override
    public void onIntegrationInitialize() {
        init();
    }
}