package com.evandev.reliable_remover.compat;

import com.evandev.reliable_remover.Constants;
import dev.emi.emi.registry.EmiRecipes;
import dev.emi.emi.registry.EmiStackList;
import dev.emi.emi.screen.EmiScreenManager;
import dev.emi.emi.search.EmiSearch;

public class EmiFastRefresh {

    public static void refresh() {
        try {
            EmiStackList.bakeFiltered();
            EmiRecipes.bake();
            EmiSearch.bake();

            if (EmiScreenManager.search != null) {
                EmiScreenManager.search.update();
            }
            EmiScreenManager.forceRecalculate();

        } catch (Throwable e) {
            Constants.LOG.error("Reliable Remover failed to execute fast EMI refresh.", e);
        }
    }
}