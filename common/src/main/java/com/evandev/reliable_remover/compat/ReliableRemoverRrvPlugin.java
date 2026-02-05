package com.evandev.reliable_remover.compat;

import cc.cassian.rrv.api.ReliableRecipeViewerPlugin;
import cc.cassian.rrv.api.recipe.ItemView;
import com.evandev.reliable_remover.Constants;
import com.evandev.reliable_remover.config.ModConfig;
import com.evandev.reliable_remover.config.RuleManager;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class ReliableRemoverRrvPlugin implements ReliableRecipeViewerPlugin {

    @Override
    public void onIntegrationInitialize() {
       init();
    }

	public static void init() {
		ItemView.addClientReloadCallback(() -> {
			ModConfig.get();
			RuleManager.load();
			BuiltInRegistries.ITEM.forEach(item -> {
				if (!ModConfig.get().removeItemsFromEmi) return;
				if (RuleManager.isHidden(item.getDefaultInstance())) {
					ItemView.excludeItem(item);
				}
			});
		});
	}
}