package com.evandev.reliable_remover;

import com.evandev.reliable_recipes.api.ReliableRecipesAPI;
import com.evandev.reliable_recipes.config.ConfigSync;
import com.evandev.reliable_remover.config.RuleLoader;
import com.evandev.reliable_remover.config.RuleManager;

public class CommonClass {
    public static void init() {
        RuleManager.load();
        RuleManager.MOD_INIT_PHASE = false;

        ReliableRecipesAPI.registerContextualItemHider(RuleManager::isHiddenFromRecipes);
        ConfigSync.register(Constants.MOD_ID, RuleLoader::createSyncSnapshot, RuleManager::onSyncedRulesChanged);
    }
}
