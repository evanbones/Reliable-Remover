package com.evandev.reliable_remover.data;

import com.google.gson.annotations.SerializedName;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

public class RemovalRule {
    public Action action;
    public Filter filter;

    public enum Action {
        @SerializedName(value = "REMOVE", alternate = {"remove", "Remove"})
        REMOVE,

        @SerializedName(value = "REMOVE_ATTACKS", alternate = {"remove_attacks", "Remove_Attacks"})
        REMOVE_ATTACKS,

        @SerializedName(value = "REMOVE_INTERACTIONS", alternate = {"remove_interactions", "Remove_Interactions"})
        REMOVE_INTERACTIONS
    }

    public static class Filter {
        public Set<String> items = new HashSet<>();
        public Set<String> dimensions = new HashSet<>();
        public Set<String> entities = new HashSet<>();
        public String pattern;

        @SerializedName(value = "mod", alternate = {"mods"})
        public Set<String> mod = new HashSet<>();

        public String nbt;
        public Filter not;

        private transient Pattern compiledPattern;
        private transient Pattern compiledNbtPattern;

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

        public boolean matches(ItemStack stack, String itemId, String dimension, String entityId) {
            if (!matchesLogic(itemId, dimension, entityId)) return false;

            if (nbt != null) {
                StringBuilder dataBuilder = new StringBuilder();

                if (stack.has(DataComponents.POTION_CONTENTS)) {
                    dataBuilder.append(Objects.requireNonNull(stack.get(DataComponents.POTION_CONTENTS)));
                }

                if (stack.has(DataComponents.CUSTOM_DATA)) {
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

            boolean hasItemFilter = (items != null && !items.isEmpty()) || pattern != null;
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

            if (pattern != null) {
                if (compiledPattern == null) {
                    String p = pattern.startsWith("/") && pattern.endsWith("/")
                            ? pattern.substring(1, pattern.length() - 1)
                            : pattern;
                    compiledPattern = Pattern.compile(p);
                }
                return compiledPattern.matcher(itemId).matches();
            }

            return false;
        }
    }
}