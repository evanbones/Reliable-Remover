package com.evandev.reliable_remover.data;

import com.google.gson.annotations.SerializedName;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.enchantment.Enchantment;

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

    @SerializedName(value = "registry", alternate = {"registries"})
    public Set<String> registry = new HashSet<>();

    @SerializedName(value = "tag_type", alternate = {"tag_types"})
    public Set<String> tagType = new HashSet<>();

    public String pattern;
    @SerializedName(value = "patterns", alternate = {"regex"})
    public List<String> patterns = new ArrayList<>();

    @SerializedName(value = "nbt", alternate = {"nbts", "components"})
    public List<String> nbt = new ArrayList<>();

    public RemovalRule not;

    private transient volatile List<Pattern> compiledPatterns;
    private transient volatile List<Pattern> compiledNbtPatterns;

    public boolean matches(ItemStack stack, String itemId, String dimension, String entityId, Holder<?> registryHolder, String context) {
        if (!matchesLogic(stack, itemId, dimension, entityId, this.action, registryHolder, context)) return false;

        if (nbt != null && !nbt.isEmpty()) {
            if (stack == null || stack.isEmpty()) return false;

            if (compiledNbtPatterns == null) {
                synchronized (this) {
                    if (compiledNbtPatterns == null) {
                        List<Pattern> list = new ArrayList<>();
                        for (String p : nbt) list.add(compile(p));
                        compiledNbtPatterns = list;
                    }
                }
            }

            String componentsStr = stack.getComponents().toString();
            String customDataStr = stack.has(DataComponents.CUSTOM_DATA)
                    ? stack.get(DataComponents.CUSTOM_DATA).getUnsafe().toString()
                    : "";

            for (Pattern p : compiledNbtPatterns) {
                if (p.matcher(componentsStr).matches() || (!customDataStr.isEmpty() && p.matcher(customDataStr).matches())) {
                    return true;
                }
            }
            return false;
        }

        return true;
    }

    private boolean matchesLogic(ItemStack stack, String itemId, String dimension, String entityId, Action currentAction, Holder<?> registryHolder, String context) {
        if (currentAction == null) currentAction = Action.REMOVE;

        if (not != null && not.matchesLogic(stack, itemId, dimension, entityId, currentAction, registryHolder, context))
            return false;

        if (context != null) {
            if (this.registry != null && !this.registry.isEmpty()) {
                boolean matchesReg = false;
                for (String reg : this.registry) {
                    if (context.contains(reg)) {
                        matchesReg = true;
                        break;
                    }
                }
                if (!matchesReg) return false;
            }

            if (this.tagType != null && !this.tagType.isEmpty()) {
                if (context.startsWith("tag:")) {
                    String type = context.substring(4);
                    if (!this.tagType.contains(type)) return false;
                }
            }
        }

        if (dimensions != null && !dimensions.isEmpty()) {
            if (dimension == null || !dimensions.contains(dimension)) return false;
        }

        if (entities != null && !entities.isEmpty()) {
            if (entityId == null || !entities.contains(entityId)) return false;
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
                    if (checkTag(filter, itemLocation, stack, currentAction, registryHolder)) return true;
                } else if (filter.equals(itemId)) {
                    return true;
                }
            }
        }

        if (hasTagFilter) {
            for (String tagId : tags) {
                if (checkTag(tagId, itemLocation, stack, currentAction, registryHolder)) return true;
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

    private boolean checkTag(String tagId, ResourceLocation itemLocation, ItemStack stack, Action currentAction, Holder<?> registryHolder) {
        String cleanTagId = tagId.startsWith("#") ? tagId.substring(1) : tagId;
        ResourceLocation tagLocation = ResourceLocation.tryParse(cleanTagId);

        if (tagLocation != null && itemLocation != null) {
            if (currentAction == Action.REMOVE_POTION) {
                TagKey<Potion> tagKey = TagKey.create(Registries.POTION, tagLocation);
                return BuiltInRegistries.POTION.getHolder(ResourceKey.create(Registries.POTION, itemLocation))
                        .map(holder -> holder.is(tagKey))
                        .orElse(false);
            } else if (currentAction == Action.REMOVE_ENCHANTMENT) {
                if (registryHolder != null) {
                    TagKey<Enchantment> tagKey = TagKey.create(Registries.ENCHANTMENT, tagLocation);
                    try {
                        @SuppressWarnings("unchecked")
                        Holder<Enchantment> enchHolder = (Holder<Enchantment>) registryHolder;
                        return enchHolder.is(tagKey);
                    } catch (ClassCastException e) {
                        return false;
                    }
                }
                return false;
            } else {
                TagKey<Item> tagKey = TagKey.create(Registries.ITEM, tagLocation);
                if (stack != null && !stack.isEmpty()) {
                    return stack.is(tagKey);
                } else {
                    return BuiltInRegistries.ITEM.getHolder(ResourceKey.create(Registries.ITEM, itemLocation))
                            .map(holder -> holder.is(tagKey))
                            .orElse(false);
                }
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