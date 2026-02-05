package com.evandev.reliable_remover.data;

import com.google.gson.annotations.SerializedName;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.component.CustomData;

import java.util.*;
import java.util.regex.Pattern;

public class RemovalRule {
    public Action action;

    @Deprecated
    @SerializedName("filter")
    public RemovalRule filter;

    public Set<String> items = new HashSet<>();
    public Set<String> dimensions = new HashSet<>();
    public Set<String> entities = new HashSet<>();

    @SerializedName(value = "mod", alternate = {"mods"})
    public Set<String> mod = new HashSet<>();

    public String pattern;
    @SerializedName(value = "patterns", alternate = {"regex"})
    public List<String> patterns = new ArrayList<>();

    public String nbt;

    public RemovalRule not;

    private transient List<Pattern> compiledPatterns;
    private transient Pattern compiledNbtPattern;

    /**
     * Merges fields from the legacy 'filter' object into this object if they are present.
     */
    public void mergeLegacy() {
        if (filter != null) {
            if (filter.items != null) this.items.addAll(filter.items);
            if (filter.dimensions != null) this.dimensions.addAll(filter.dimensions);
            if (filter.entities != null) this.entities.addAll(filter.entities);
            if (filter.mod != null) this.mod.addAll(filter.mod);
            if (filter.patterns != null) this.patterns.addAll(filter.patterns);

            if (this.pattern == null) this.pattern = filter.pattern;
            if (this.nbt == null) this.nbt = filter.nbt;
            if (this.not == null) this.not = filter.not;

            this.filter = null;
        }
    }

    public boolean matches(String itemId) {
        return matches(itemId, null);
    }

    public boolean matches(String itemId, String dimension) {
        return matches(itemId, dimension, null);
    }

    public boolean matches(String itemId, String dimension, String entityId) {
        if (nbt != null) return false;
        return matchesLogic(itemId, dimension, entityId);
    }

    public boolean matches(ItemStackTemplate stack, String itemId, String dimension, String entityId) {
        if (!matchesLogic(itemId, dimension, entityId)) return false;

        if (nbt != null) {
            StringBuilder dataBuilder = new StringBuilder();

            if (stack.get(DataComponents.POTION_CONTENTS) != null) {
                dataBuilder.append(Objects.requireNonNull(stack.get(DataComponents.POTION_CONTENTS)));
            }

            if (stack.get(DataComponents.CUSTOM_DATA) != null) {
                dataBuilder.append(Objects.requireNonNull(stack.get(DataComponents.CUSTOM_DATA)));
            }

            if (dataBuilder.isEmpty()) return false;

            if (compiledNbtPattern == null) compiledNbtPattern = Pattern.compile(nbt);

            return compiledNbtPattern.matcher(dataBuilder.toString()).matches();
        }

        return true;
    }

    private boolean matchesLogic(String itemId, String dimension, String entityId) {
        if (not != null && not.matchesLogic(itemId, dimension, entityId)) return false;

        if (dimensions != null && !dimensions.isEmpty()) {
            if (dimension == null) return false;
            if (!dimensions.contains(dimension)) return false;
        }

        if (entities != null && !entities.isEmpty()) {
            if (entityId == null) return false;
            if (!entities.contains(entityId)) return false;
        }

        boolean hasLegacyPattern = pattern != null && !pattern.isEmpty();
        boolean hasPatternList = patterns != null && !patterns.isEmpty();
        boolean hasItemFilter = (items != null && !items.isEmpty()) || hasLegacyPattern || hasPatternList;
        boolean hasModFilter = (mod != null && !mod.isEmpty());

        if (!hasItemFilter && !hasModFilter) {
            return (dimensions != null && !dimensions.isEmpty()) ||
                    (entities != null && !entities.isEmpty());
        }

        if (hasModFilter) {
            String[] split = itemId.split(":");
            if (split.length < 2 || !mod.contains(split[0])) return false;
            if (!hasItemFilter) return true;
        }

        if (items != null && items.contains(itemId)) return true;

        if (hasLegacyPattern || hasPatternList) {
            if (compiledPatterns == null) {
                compiledPatterns = new ArrayList<>();
                if (hasLegacyPattern) compiledPatterns.add(compile(pattern));
                if (hasPatternList) {
                    for (String p : patterns) compiledPatterns.add(compile(p));
                }
            }
            for (Pattern p : compiledPatterns) {
                if (p.matcher(itemId).matches()) return true;
            }
        }

        return false;
    }

    private Pattern compile(String regex) {
        String p = regex.startsWith("/") && regex.endsWith("/")
                ? regex.substring(1, regex.length() - 1)
                : regex;
        return Pattern.compile(p);
    }

    public enum Action {
        @SerializedName(value = "REMOVE", alternate = {"remove", "Remove"})
        REMOVE,
        @SerializedName(value = "REMOVE_ATTACKS", alternate = {"remove_attacks", "Remove_Attacks"})
        REMOVE_ATTACKS,
        @SerializedName(value = "REMOVE_INTERACTIONS", alternate = {"remove_interactions", "Remove_Interactions"})
        REMOVE_INTERACTIONS,
        @SerializedName(value = "REMOVE_ENCHANTMENT", alternate = {"remove_enchantment", "Remove_Enchantment"})
        REMOVE_ENCHANTMENT,
        @SerializedName(value = "REMOVE_POTION", alternate = {"remove_potion", "Remove_Potion"})
        REMOVE_POTION
    }
}