package com.evandev.reliable_remover;

import com.evandev.reliable_recipes.api.ReliableRecipesAPI;
import com.evandev.reliable_recipes.config.ConfigSync;
import com.evandev.reliable_remover.config.RuleManager;

public class CommonClass {
    public static void init() {
        RuleManager.load();
        RuleManager.MOD_INIT_PHASE = false;

        ReliableRecipesAPI.registerContextualItemHider(RuleManager::isHidden);
        ConfigSync.register(Constants.MOD_ID, RuleManager::createSyncSnapshot, RuleManager::onSyncedRulesChanged);
    }
}
