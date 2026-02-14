package com.evandev.reliable_remover.data;

import com.google.gson.annotations.SerializedName;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;

import java.util.*;
import java.util.regex.Pattern;

public class RemovalRule {
    public Action action;

    public Set<String> items = new HashSet<>();
    public Set<String> dimensions = new HashSet<>();
    public Set<String> entities = new HashSet<>();

    @SerializedName(value = "mod", alternate = {"mods"})
    public Set<String> mod = new HashSet<>();

    @SerializedName(value = "tag", alternate = {"tags"})
    public Set<String> tags = new HashSet<>();

    public String pattern;
    @SerializedName(value = "patterns", alternate = {"regex"})
    public List<String> patterns = new ArrayList<>();

    public String nbt;

    public RemovalRule not;

    private transient List<Pattern> compiledPatterns;
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

        boolean hasPattern = pattern != null && !pattern.isEmpty();
        boolean hasPatternList = patterns != null && !patterns.isEmpty();
        boolean hasTagFilter = tags != null && !tags.isEmpty();
        boolean hasItemFilter = (items != null && !items.isEmpty()) || hasPattern || hasPatternList || hasTagFilter;
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

        ResourceLocation itemLocation = ResourceLocation.tryParse(itemId);

        if (items != null && !items.isEmpty()) {
            for (String filter : items) {
                if (filter.startsWith("#")) {
                    String tagId = filter.substring(1);
                    ResourceLocation tagLocation = ResourceLocation.tryParse(tagId);

                    if (tagLocation != null && itemLocation != null) {
                        boolean isHandled = BuiltInRegistries.ITEM.getHolder(itemLocation)
                                .map(holder -> holder.is(TagKey.create(Registries.ITEM, tagLocation)))
                                .orElse(false);

                        if (isHandled) return true;
                    }
                } else if (filter.equals(itemId)) {
                    return true;
                }
            }
        }

        if (hasTagFilter) {
            for (String tagId : tags) {
                String cleanTagId = tagId.startsWith("#") ? tagId.substring(1) : tagId;
                ResourceLocation tagLocation = ResourceLocation.tryParse(cleanTagId);

                if (tagLocation != null && itemLocation != null) {
                    boolean isHandled = BuiltInRegistries.ITEM.getHolder(itemLocation)
                            .map(holder -> holder.is(TagKey.create(Registries.ITEM, tagLocation)))
                            .orElse(false);
                    if (isHandled) return true;
                }
            }
        }

        if (hasPattern || hasPatternList) {
            if (compiledPatterns == null) {
                compiledPatterns = new ArrayList<>();
                if (hasPattern) compiledPatterns.add(compile(pattern));
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

}