package com.evandev.reliable_remover.config;

import com.evandev.reliable_remover.data.RemovalRule;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rules for one action, indexed by the ids they name
 */
final class RuleIndex {
    private final int[] general;
    private final Map<String, int[]> keyed;
    private final RemovalRule[] rules;
    private final Map<String, List<RemovalRule>> candidates = new ConcurrentHashMap<>();
    private final List<RemovalRule> generalOnly;

    RuleIndex(List<RemovalRule> ruleList) {
        this.rules = ruleList.toArray(new RemovalRule[0]);
        List<Integer> generalList = new ArrayList<>();
        Map<String, List<Integer>> keyedLists = new HashMap<>();
        for (int i = 0; i < rules.length; i++) {
            Set<String> keys = rules[i].exactIdKeys();
            if (keys == null) {
                generalList.add(i);
            } else {
                for (String key : keys) keyedLists.computeIfAbsent(key, k -> new ArrayList<>()).add(i);
            }
        }
        this.general = toArray(generalList);
        this.keyed = new HashMap<>();
        keyedLists.forEach((key, list) -> keyed.put(key, toArray(list)));
        this.generalOnly = merge(new int[0]);
    }

    List<RemovalRule> candidates(String id) {
        if (id == null) return generalOnly;
        int[] specific = keyed.get(id);
        if (specific == null) return generalOnly;
        return candidates.computeIfAbsent(id, k -> merge(specific));
    }

    private List<RemovalRule> merge(int[] specific) {
        List<RemovalRule> out = new ArrayList<>(general.length + specific.length);
        int g = 0, s = 0;
        while (g < general.length || s < specific.length) {
            if (s >= specific.length || (g < general.length && general[g] < specific[s])) {
                out.add(rules[general[g++]]);
            } else {
                out.add(rules[specific[s++]]);
            }
        }
        return List.copyOf(out);
    }

    private static int[] toArray(List<Integer> list) {
        int[] out = new int[list.size()];
        for (int i = 0; i < out.length; i++) out[i] = list.get(i);
        return out;
    }
}
