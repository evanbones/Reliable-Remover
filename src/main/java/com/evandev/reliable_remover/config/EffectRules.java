package com.evandev.reliable_remover.config;

import com.evandev.reliable_remover.data.Action;
import com.evandev.reliable_remover.data.RemovalRule;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

import java.util.HashSet;
import java.util.Set;

public class EffectRules {
    public static boolean isEffectBlocked(Holder<MobEffect> effectHolder, Level level) {
        return isEffectBlocked(effectHolder, level, null);
    }

    public static boolean isEffectBlocked(Holder<MobEffect> effectHolder, Level level, Entity entity) {
        String id = effectId(effectHolder);
        if (id == null) return false;
        if (RuleManager.isGloballyRemoved(id)) return true;
        if (getEffectReplacement(effectHolder, level, entity) != null) return false;
        String dim = RuleManager.dimensionId(level);
        if (RuleManager.checkRules(null, id, Action.REMOVE_EFFECT, dim, entity, effectHolder, "effect")) return true;
        return RuleManager.checkRules(null, id, Action.REMOVE, dim, entity, effectHolder, "effect");
    }

    public static boolean isEffectCreativeBlocked(Holder<MobEffect> effectHolder) {
        return isEffectCreativeBlocked(effectHolder, null);
    }

    public static boolean isEffectCreativeBlocked(Holder<MobEffect> effectHolder, Entity entity) {
        String id = effectId(effectHolder);
        if (id == null) return false;
        if (RuleManager.isGloballyRemoved(id)) return true;
        if (RuleManager.checkRules(null, id, Action.REMOVE_CREATIVE, null, entity, effectHolder, "creative")) return true;
        if (RuleManager.checkRules(null, id, Action.REMOVE_EFFECT, null, entity, effectHolder, "effect")) return true;
        return RuleManager.checkRules(null, id, Action.REMOVE, null, entity, effectHolder, "effect");
    }

    public static Holder<MobEffect> getEffectReplacement(Holder<MobEffect> effectHolder, Level level, Entity entity) {
        if (effectHolder == null) return null;
        String dim = RuleManager.dimensionId(level);
        Set<String> seen = new HashSet<>();
        Holder<MobEffect> current = effectHolder;
        Holder<MobEffect> replacement = null;
        while (true) {
            String id = effectId(current);
            if (id == null) return replacement;
            if (!seen.add(id)) return null;

            RemovalRule rule = RuleManager.getMatchingRule(null, id, Action.REMOVE_EFFECT, dim, entity, current, "effect");
            if (rule == null) rule = RuleManager.getMatchingRule(null, id, Action.REMOVE, dim, entity, current, "effect");
            if (rule == null || !rule.hasReplacement()) return replacement;

            Identifier replacementId = Identifier.tryParse(rule.replaceWith.trim());
            if (replacementId == null) return replacement;
            Holder<MobEffect> next = RemovalRule.getRegistryHolder(BuiltInRegistries.MOB_EFFECT, replacementId).orElse(null);
            if (next == null) return replacement;
            replacement = next;
            current = next;
        }
    }

    //? if <1.21 {
    /*public static boolean isEffectBlocked(MobEffect effect, Level level, Entity entity) {
        return isEffectBlocked(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect), level, entity);
    }

    public static boolean isEffectCreativeBlocked(MobEffect effect, Entity entity) {
        return isEffectCreativeBlocked(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect), entity);
    }

    public static Holder<MobEffect> getEffectReplacement(MobEffect effect, Level level, Entity entity) {
        return getEffectReplacement(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect), level, entity);
    }
    *///?}

    private static String effectId(Holder<MobEffect> effectHolder) {
        if (effectHolder == null) return null;
        Identifier id = BuiltInRegistries.MOB_EFFECT.getKey(effectHolder.value());
        return id != null ? id.toString() : null;
    }
}
