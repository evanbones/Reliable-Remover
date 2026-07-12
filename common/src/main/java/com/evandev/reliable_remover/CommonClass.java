package com.evandev.reliable_remover;

import com.evandev.reliable_recipes.api.ReliableRecipesAPI;
import com.evandev.reliable_remover.config.RuleManager;
import com.evandev.reliable_remover.data.RemovalRule;

import java.util.List;

public class CommonClass {
    public static void init() {
        RuleManager.load();
        RuleManager.MOD_INIT_PHASE = false;

        for (List<RemovalRule> rules : RuleManager.getRulesByAction().values()) {
            for (RemovalRule rule : rules) {
                if (rule.replaceWith != null && !rule.replaceWith.isEmpty()) {
                    if (rule.items != null) {
                        for (String item : rule.items) {
                            ReliableRecipesAPI.registerItemReplacement(item, rule.replaceWith);
                        }
                    }
                }
            }
        }

        ReliableRecipesAPI.registerContextualItemHider((stack, context) -> RuleManager.isHidden(stack, null, null, context));
    }
}