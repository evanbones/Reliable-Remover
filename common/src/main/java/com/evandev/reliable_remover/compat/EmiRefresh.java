package com.evandev.reliable_remover.compat;

import com.evandev.reliable_remover.Constants;
import dev.emi.emi.runtime.EmiReloadManager;

public class EmiRefresh {

    public static void refresh() {
        try {
            EmiReloadManager.reload();
        } catch (Throwable e) {
            Constants.LOG.error("Reliable Remover failed to execute EMI refresh.", e);
        }
    }
}