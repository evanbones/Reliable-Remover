package com.evandev.reliable_remover.data;

import com.evandev.reliable_remover.Constants;
import com.google.gson.annotations.SerializedName;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

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

    @SerializedName(value = "replace_with", alternate = {"replacement"})
    public String replaceWith;

    public RemovalRule not;

    private transient volatile List<Pattern> compiledPatterns;
    private transient volatile List<Pattern> compiledComponentRegex;
    private transient volatile Map<String, TagKey<Item>> compiledItemTags;
    private transient volatile Map<String, TagKey<Potion>> compiledPotionTags;
    private transient volatile Map<String, TagKey<Enchantment>> compiledEnchTags;

    public boolean matches(ItemStack stack, String itemId, String dimension, String entityId, Holder<?> registryHolder, String context) {
        if (!matchesLogic(stack, itemId, dimension, entityId, this.action, registryHolder, context)) return false;

        if (nbt != null && !nbt.isEmpty()) {
            if (stack == null || stack.isEmpty()) return false;

            if (compiledComponentRegex == null) {
                synchronized (this) {
                    if (compiledComponentRegex == null) {
                        List<Pattern> list = new ArrayList<>();
                        for (String p : nbt) {
                            Pattern compiled = compile(p);
                            if (compiled != null) list.add(compiled);
                        }
                        compiledComponentRegex = list;
                    }
                }
            }

            if (compiledComponentRegex.isEmpty()) return false;

            String componentsStr = stack.getComponents().toString();
            String customDataStr = stack.has(DataComponents.CUSTOM_DATA)
                    ? Objects.requireNonNull(stack.get(DataComponents.CUSTOM_DATA)).copyTag().toString()
                    : "";

            for (Pattern p : compiledComponentRegex) {
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

        Identifier itemLocation = Identifier.tryParse(itemId);

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

    private boolean checkTag(String tagId, Identifier itemLocation, ItemStack stack, Action currentAction, Holder<?> registryHolder) {
        String cleanTagId = tagId.startsWith("#") ? tagId.substring(1) : tagId;
        Identifier tagLocation = Identifier.tryParse(cleanTagId);

        if (tagLocation != null && itemLocation != null) {
            if (currentAction == Action.REMOVE_POTION) {
                if (compiledPotionTags == null) {
                    synchronized (this) {
                        if (compiledPotionTags == null) compiledPotionTags = new java.util.HashMap<>();
                    }
                }
                TagKey<Potion> tagKey = compiledPotionTags.computeIfAbsent(cleanTagId, k -> TagKey.create(Registries.POTION, tagLocation));
                return BuiltInRegistries.POTION.get(itemLocation)
                        .map(holder -> holder.is(tagKey))
                        .orElse(false);

            } else if (currentAction == Action.REMOVE_ENCHANTMENT) {
                if (registryHolder != null) {
                    if (compiledEnchTags == null) {
                        synchronized (this) {
                            if (compiledEnchTags == null) compiledEnchTags = new java.util.HashMap<>();
                        }
                    }
                    TagKey<Enchantment> tagKey = compiledEnchTags.computeIfAbsent(cleanTagId, k -> TagKey.create(Registries.ENCHANTMENT, tagLocation));
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
                if (compiledItemTags == null) {
                    synchronized (this) {
                        if (compiledItemTags == null) compiledItemTags = new java.util.HashMap<>();
                    }
                }
                TagKey<Item> tagKey = compiledItemTags.computeIfAbsent(cleanTagId, k -> TagKey.create(Registries.ITEM, tagLocation));
                if (stack != null && !stack.isEmpty()) {
                    return stack.is(tagKey);
                } else {
                    return BuiltInRegistries.ITEM.get(itemLocation)
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
        try {
            return Pattern.compile(p);
        } catch (PatternSyntaxException e) {
            Constants.LOG.error("Reliable Remover: Invalid regex pattern found in config: '{}'. Skipping this pattern.", regex);
            return null;
        }
    }
}