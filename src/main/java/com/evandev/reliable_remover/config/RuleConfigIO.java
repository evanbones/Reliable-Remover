package com.evandev.reliable_remover.config;

import java.util.List;

public class RuleConfigIO {

    public static boolean addRemovalRule(String itemId) {
        List<String> blacklist = ModConfig.get().blacklistedItems;
        if (!blacklist.contains(itemId)) {
            blacklist.add(itemId);
            ModConfig.save();
            RuleManager.load();
            return true;
        }
        return false;
    }

    public static boolean removeRemovalRule(String itemId) {
        List<String> blacklist = ModConfig.get().blacklistedItems;
        if (blacklist.contains(itemId)) {
            blacklist.remove(itemId);
            ModConfig.save();
            RuleManager.load();
            return true;
        }
        return false;
    }
}