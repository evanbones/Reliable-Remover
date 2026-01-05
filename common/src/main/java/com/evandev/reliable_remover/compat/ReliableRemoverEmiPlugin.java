package com.evandev.reliable_remover.compat;

import com.evandev.reliable_remover.config.ModConfig;
import com.evandev.reliable_remover.config.RuleManager;
import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiInitRegistry;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;

@EmiEntrypoint
public class ReliableRemoverEmiPlugin implements EmiPlugin {
    @Override
    public void initialize(EmiInitRegistry registry) {
        ModConfig.get();
        RuleManager.load();

        if (!ModConfig.get().removeItemsFromEmi) return;

        registry.disableStacks(emiStack -> RuleManager.isHidden(emiStack.getItemStack()));
    }

    @Override
    public void register(EmiRegistry registry) {
    }
}