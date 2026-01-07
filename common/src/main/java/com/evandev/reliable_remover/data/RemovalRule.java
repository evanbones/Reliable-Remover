package com.evandev.reliable_remover.data;

import com.google.gson.annotations.SerializedName;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

import java.util.HashSet;
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
        public String pattern;
        public String mod;
        public String nbt;
        public Filter not;

        private transient Pattern compiledPattern;
        private transient Pattern compiledNbtPattern;

        public boolean matches(String itemId) {
            return matches(itemId, null);
        }

        public boolean matches(String itemId, String dimension) {
            if (nbt != null) return false;
            return matchesLogic(itemId, dimension);
        }

        public boolean matches(ItemStack stack, String itemId, String dimension) {
            if (!matchesLogic(itemId, dimension)) return false;

            if (nbt != null) {
                if (!stack.hasTag()) return false;

                CompoundTag data = stack.getTag();
                if (data == null) return false;

                if (compiledNbtPattern == null) compiledNbtPattern = Pattern.compile(nbt);

                return compiledNbtPattern.matcher(data.toString()).matches();
            }

            return true;
        }

        private boolean matchesLogic(String itemId, String dimension) {
            if (not != null && not.matchesLogic(itemId, dimension)) return false;

            if (dimensions != null && !dimensions.isEmpty()) {
                if (dimension == null) return false;
                if (!dimensions.contains(dimension)) return false;
            }

            boolean hasItemFilter = (items != null && !items.isEmpty()) || pattern != null;

            if (mod != null) {
                String[] split = itemId.split(":");
                if (split.length < 2 || !split[0].equals(mod)) return false;

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
                if (compiledPattern.matcher(itemId).matches()) return true;
            }

            if (hasItemFilter) return false;

            return true;
        }
    }
}