package com.evandev.reliable_remover;

import com.evandev.reliable_remover.config.RuleManager;

public class CommonClass {

    public static void init() {
        RuleManager.load();
    }
}