package com.evandev.reliable_remover.data;

import com.google.gson.JsonElement;
import java.util.regex.Pattern;
import java.util.HashSet;
import java.util.Set;

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
        public Filter not;

        private transient Pattern compiledPattern;

        public boolean matches(String itemId) {
            // Negation
            if (not != null && not.matches(itemId)) return false;

            // Mod ID
            if (mod != null) {
                String itemMod = itemId.split(":")[0];
                if (!itemMod.equals(mod)) return false;
            }

            // Direct match
            if (items != null && items.contains(itemId)) return true;

            // Regex
            if (pattern != null) {
                if (compiledPattern == null) {
                    String p = pattern.startsWith("/") && pattern.endsWith("/")
                            ? pattern.substring(1, pattern.length() - 1)
                            : pattern;
                    compiledPattern = Pattern.compile(p);
                }
                if (compiledPattern.matcher(itemId).matches()) return true;
            }

            return false;
        }
    }
}