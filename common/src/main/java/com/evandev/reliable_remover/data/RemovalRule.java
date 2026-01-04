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
        public String pattern;
        public String mod;
        public String nbt;
        public Filter not;

        private transient Pattern compiledPattern;
        private transient Pattern compiledNbtPattern;

        public boolean matches(String itemId) {
            if (nbt != null) return false;
            return matchesLogic(itemId);
        }

        public boolean matches(ItemStack stack, String itemId) {
            if (!matchesLogic(itemId)) return false;

            if (nbt != null) {
                if (!stack.hasTag()) return false;

                CompoundTag data = stack.getTag();
                if (data == null) return false;

                if (compiledNbtPattern == null) compiledNbtPattern = Pattern.compile(nbt);

                return compiledNbtPattern.matcher(data.toString()).matches();
            }

            return true;
        }

        private boolean matchesLogic(String itemId) {
            if (not != null && not.matchesLogic(itemId)) return false;

            if (mod != null) {
                String[] split = itemId.split(":");
                if (split.length < 2 || !split[0].equals(mod)) return false;
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

            return mod != null && (items == null || items.isEmpty());
        }
    }
}