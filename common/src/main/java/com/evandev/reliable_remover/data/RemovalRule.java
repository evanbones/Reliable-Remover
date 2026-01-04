package com.evandev.reliable_remover.data;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

public class RemovalRule {
    public Action action;
    public Filter filter;

    public enum Action {
        REMOVE,
        REMOVE_ATTACKS,
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
                if (!stack.has(DataComponents.CUSTOM_DATA)) return false;

                CustomData data = stack.get(DataComponents.CUSTOM_DATA);
                if (data == null) return false;

                if (compiledNbtPattern == null) compiledNbtPattern = Pattern.compile(nbt);

                return compiledNbtPattern.matcher(data.toString()).matches();
            }

            return true;
        }

        private boolean matchesLogic(String itemId) {
            if (not != null && not.matchesLogic(itemId)) return false;

            if (mod != null) {
                String itemMod = itemId.split(":")[0];
                if (!itemMod.equals(mod)) return false;
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

            return mod != null && items.isEmpty() && pattern == null;
        }
    }
}