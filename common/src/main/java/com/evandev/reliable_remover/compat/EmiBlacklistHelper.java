package com.evandev.reliable_remover.compat;

import com.evandev.reliable_remover.config.ModConfig;
import com.evandev.reliable_remover.config.RuleManager;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.ItemStack;

public class EmiBlacklistHelper {

    public static boolean isEmiStackBlacklisted(EmiStack stack) {
        if (stack == null || stack.isEmpty()) return false;

        ResourceLocation id = stack.getId();
        if (id != null) {
            if (ModConfig.get().blacklistedItems.contains(id.toString())) {
                return true;
            }

            if (id.getNamespace().equals("jeed")) {
                for (String blacklisted : ModConfig.get().blacklistedItems) {
                    ResourceLocation blId = ResourceLocation.tryParse(blacklisted);
                    if (blId != null && id.getPath().contains(blId.getPath())) {
                        return true;
                    }
                }
                for (var entry : BuiltInRegistries.MOB_EFFECT.entrySet()) {
                    ResourceLocation effectId = entry.getKey().location();
                    if (id.getPath().contains(effectId.getPath()) || id.toString().contains(effectId.getPath())) {
                        if (RuleManager.isEffectCreativeBlocked(entry.getValue())) {
                            return true;
                        }
                    }
                }
            }
        }

        Object key = stack.getKey();
        if (key instanceof Holder<?> holder && holder.value() instanceof MobEffect effect) {
            return RuleManager.isEffectCreativeBlocked(effect);
        } else if (key instanceof MobEffect effect) {
            return RuleManager.isEffectCreativeBlocked(effect);
        }

        ItemStack itemStack = stack.getItemStack();
        if ((itemStack == null || itemStack.isEmpty()) && id != null) {
            return RuleManager.isFluidHidden(id.toString());
        }

        return false;
    }
}
