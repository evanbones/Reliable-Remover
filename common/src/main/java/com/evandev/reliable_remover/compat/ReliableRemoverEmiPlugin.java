package com.evandev.reliable_remover.compat;

import com.evandev.reliable_remover.config.RuleManager;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.core.registries.BuiltInRegistries;

public class ReliableRemoverEmiPlugin implements EmiPlugin {
    @Override
    public void register(EmiRegistry registry) {
        BuiltInRegistries.ITEM.forEach(item -> {
            if (RuleManager.isHidden(BuiltInRegistries.ITEM.getKey(item).toString())) {
                registry.removeEmiStacks(EmiStack.of(item));
            }
        });

    }
}