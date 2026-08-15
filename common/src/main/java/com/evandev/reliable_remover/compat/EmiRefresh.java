package com.evandev.reliable_remover.compat;

import com.evandev.reliable_remover.Constants;
import dev.emi.emi.runtime.EmiReloadManager;
import net.minecraft.client.Minecraft;

public class EmiRefresh {

    public static void refresh() {
        try {
            Minecraft mc = Minecraft.getInstance();
            mc.execute(() -> {
                try {
                    EmiReloadManager.reload();
                } catch (Throwable e) {
                    Constants.LOG.error("Reliable Remover failed to execute EMI refresh.", e);
                }
            });
        } catch (Throwable e) {
            Constants.LOG.error("Reliable Remover failed to schedule EMI refresh.", e);
        }
    }
}