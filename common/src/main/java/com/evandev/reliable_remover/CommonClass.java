package com.evandev.reliable_remover;

import com.evandev.reliable_recipes.api.ReliableRecipesAPI;
import com.evandev.reliable_remover.config.RuleManager;
import com.evandev.reliable_remover.data.RemovalRule;

import java.util.List;

public class CommonClass {
    public static void init() {
        RuleManager.load();

        ReliableRecipesAPI.registerContextualItemHider((stack, context) -> RuleManager.isHidden(stack, null, null, context));
    }
}