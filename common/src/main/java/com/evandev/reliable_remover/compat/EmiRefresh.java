package com.evandev.reliable_remover.compat;

import com.evandev.reliable_remover.Constants;
import dev.emi.emi.registry.EmiStackList;
import dev.emi.emi.runtime.EmiReloadManager;
import dev.emi.emi.screen.EmiScreenManager;

public class EmiRefresh {

    public static void refresh() {
        try {
            EmiReloadManager.reload();
            EmiStackList.bakeFiltered();

            if (EmiScreenManager.search != null) {
                EmiScreenManager.search.update();
            }
            EmiScreenManager.forceRecalculate();

        } catch (Throwable e) {
            Constants.LOG.error("Reliable Remover failed to execute EMI refresh.", e);
        }
    }
}