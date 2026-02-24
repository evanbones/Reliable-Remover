package com.evandev.reliable_remover.data;

import com.google.gson.annotations.SerializedName;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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

    @SerializedName(value = "nbt", alternate = {"nbts"})
    public List<String> nbt = new ArrayList<>();

    public RemovalRule not;

    private transient volatile List<Pattern> compiledPatterns;
    private transient volatile List<Pattern> compiledNbtPatterns;

    public boolean matches(String itemId) {
        return matches(itemId, null);
    }

    public boolean matches(String itemId, String dimension) {
        return matches(itemId, dimension, null);
    }

    public boolean matches(String itemId, String dimension, String entityId) {
        if (nbt != null && !nbt.isEmpty()) return false;
        return matchesLogic(null, itemId, dimension, entityId);
    }

    public boolean matches(ItemStack stack, String itemId, String dimension, String entityId) {
        if (!matchesLogic(stack, itemId, dimension, entityId)) return false;

        if (nbt != null && !nbt.isEmpty()) {
            if (!stack.hasTag()) return false;

            if (compiledNbtPatterns == null) {
                synchronized (this) {
                    if (compiledNbtPatterns == null) {
                        List<Pattern> list = new ArrayList<>();
                        for (String p : nbt) {
                            list.add(compile(p));
                        }
                        compiledNbtPatterns = list;
                    }
                }
            }

            String dataStr = stack.getTag().toString();
            for (Pattern p : compiledNbtPatterns) {
                if (p.matcher(dataStr).matches()) return true;
            }
            return false;
        }

        return true;
    }

    private boolean matchesLogic(ItemStack stack, String itemId, String dimension, String entityId) {
        if (not != null && not.matchesLogic(stack, itemId, dimension, entityId)) return false;

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
        boolean hasNbtFilter = nbt != null && !nbt.isEmpty();
        boolean hasItemFilter = (items != null && !items.isEmpty()) || hasPattern || hasPatternList || hasTagFilter;
        boolean hasModFilter = (mod != null && !mod.isEmpty());

        if (!hasItemFilter && !hasModFilter) {
            return (dimensions != null && !dimensions.isEmpty()) ||
                    (entities != null && !entities.isEmpty()) ||
                    hasNbtFilter;
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
                        TagKey<Item> tagKey = TagKey.create(Registries.ITEM, tagLocation);
                        boolean isHandled;
                        if (stack != null && !stack.isEmpty()) {
                            isHandled = stack.is(tagKey);
                        } else {
                            isHandled = BuiltInRegistries.ITEM.getHolder(ResourceKey.create(Registries.ITEM, itemLocation))
                                    .map(holder -> holder.is(tagKey))
                                    .orElse(false);
                        }
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
                    TagKey<Item> tagKey = TagKey.create(Registries.ITEM, tagLocation);
                    boolean isHandled;
                    if (stack != null && !stack.isEmpty()) {
                        isHandled = stack.is(tagKey);
                    } else {
                        isHandled = BuiltInRegistries.ITEM.getHolder(ResourceKey.create(Registries.ITEM, itemLocation))
                                .map(holder -> holder.is(tagKey))
                                .orElse(false);
                    }
                    if (isHandled) return true;
                }
            }
        }

        if (hasPattern || hasPatternList) {
            if (compiledPatterns == null) {
                synchronized (this) {
                    if (compiledPatterns == null) {
                        List<Pattern> list = new ArrayList<>();
                        if (hasPattern) list.add(compile(pattern));
                        if (hasPatternList) {
                            for (String p : patterns) list.add(compile(p));
                        }
                        compiledPatterns = list;
                    }
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