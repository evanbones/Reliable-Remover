package com.evandev.reliable_remover.compat;

//? if <=1.21.1 {
/*import com.evandev.reliable_remover.config.ModConfig;
import com.evandev.reliable_remover.config.RuleManager;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.registry.EmiStackList;
import dev.emi.emi.runtime.EmiReloadManager;
import dev.emi.emi.search.EmiSearch;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class EmiBlacklistHelper {
    private static final Map<EmiStack, Boolean> REMOVED = new ConcurrentHashMap<>();
    private static boolean refreshHookRegistered;

    public static boolean isRemoved(EmiIngredient ingredient) {
        if (!ModConfig.get().removeItemsFromRecipeViewers) return false;

        List<EmiStack> stacks = ingredient.getEmiStacks();
        if (stacks.isEmpty()) return false;
        for (EmiStack stack : stacks) {
            if (!REMOVED.computeIfAbsent(stack, EmiBlacklistHelper::computeRemoved)) return false;
        }
        return true;
    }

    private static boolean computeRemoved(EmiStack stack) {
        if (isEmiStackBlacklisted(stack)) return true;
        ItemStack itemStack = stack.getItemStack();
        return itemStack != null && !itemStack.isEmpty() && RuleManager.isRecipeViewerBlocked(itemStack);
    }

    public static void onReload() {
        REMOVED.clear();
        if (!refreshHookRegistered) {
            refreshHookRegistered = true;
            RuleManager.registerSyncedRulesChanged(() -> Minecraft.getInstance().execute(EmiBlacklistHelper::refresh));
        }
    }

    public static void refresh() {
        REMOVED.clear();
        if (!EmiReloadManager.isLoaded()) return;
        EmiStackList.bakeFiltered();
        EmiSearch.update();
    }

    public static boolean isEmiStackBlacklisted(EmiStack stack) {
        if (stack == null || stack.isEmpty()) return false;

        Identifier id = stack.getId();
        if (id != null) {
            if (RuleManager.getBlacklistedItems().contains(id.toString())) {
                return true;
            }

            if (id.getNamespace().equals("jeed")) {
                for (String blacklisted : RuleManager.getBlacklistedItems()) {
                    Identifier blId = Identifier.tryParse(blacklisted);
                    if (blId != null && id.getPath().contains(blId.getPath())) {
                        return true;
                    }
                }
                for (var entry : BuiltInRegistries.MOB_EFFECT.entrySet()) {
                    Identifier effectId = entry.getKey().identifier();
                    if (id.getPath().contains(effectId.getPath()) || id.toString().contains(effectId.getPath())) {
                        Holder<MobEffect> holder = BuiltInRegistries.MOB_EFFECT.wrapAsHolder(entry.getValue());
                        if (RuleManager.isEffectCreativeBlocked(holder)) {
                            return true;
                        }
                    }
                }
            }
        }

        Object key = stack.getKey();
        if (key instanceof Holder<?> holder && holder.value() instanceof MobEffect effect) {
            Holder<MobEffect> mobEffectHolder = BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect);
            return RuleManager.isEffectCreativeBlocked(mobEffectHolder);
        } else if (key instanceof MobEffect effect) {
            Holder<MobEffect> mobEffectHolder = BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect);
            return RuleManager.isEffectCreativeBlocked(mobEffectHolder);
        }

        ItemStack itemStack = stack.getItemStack();
        if ((itemStack == null || itemStack.isEmpty()) && id != null) {
            return RuleManager.isFluidHidden(id.toString());
        }

        return false;
    }
}
*///?}
