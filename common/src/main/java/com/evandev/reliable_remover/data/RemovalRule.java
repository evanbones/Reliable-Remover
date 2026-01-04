package com.evandev.reliable_remover.data;

import net.minecraft.world.item.ItemStack;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

public class RemovalRule {
    public Action action;
    public Filter filter;

    public enum Action {
        REMOVE,
        REMOVE_ATTACKS,
        REMOVE_INTERACTIONS,
        REMOVE_NBT
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
            if (not != null && not.matches(itemId)) return false;
            if (nbt != null) return false;
            return matchesIdLogic(itemId);
        }

        public boolean matches(ItemStack stack, String itemId) {
            if (not != null && not.matches(stack, itemId)) return false;

            if (!matchesIdLogic(itemId)) return false;

            if (nbt != null) {
                if (!stack.hasTag()) return false;
                if (compiledNbtPattern == null) compiledNbtPattern = Pattern.compile(nbt);
                return compiledNbtPattern.matcher(stack.getTag().toString()).matches();
            }

            return true;
        }

        private boolean matchesIdLogic(String itemId) {
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
            return false;
        }
    }
}