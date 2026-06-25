package com.evandev.reliable_remover;

import com.evandev.reliable_recipes.api.ReliableRecipesAPI;
import com.evandev.reliable_remover.config.RuleManager;

public class CommonClass {
    public static void init() {
        RuleManager.load();

        ReliableRecipesAPI.registerContextualItemHider(RuleManager::isHidden);
    }
}