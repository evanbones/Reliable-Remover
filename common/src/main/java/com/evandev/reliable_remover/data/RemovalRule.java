package com.evandev.reliable_remover.data;

import com.evandev.reliable_remover.Constants;
import com.evandev.reliable_remover.config.RuleManager;
import com.google.gson.annotations.SerializedName;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class RemovalRule {
    public Action action;

    @SerializedName(value = "actions", alternate = {"action_list"})
    public Set<Action> actions = new HashSet<>();

    @SerializedName(value = "items", alternate = {"item", "enchantments", "enchantment"})
    public volatile Set<String> items = new HashSet<>();

    @SerializedName(value = "blocks", alternate = {"block"})
    public Set<String> blocks = new HashSet<>();

    @SerializedName(value = "fluids", alternate = {"fluid"})
    public Set<String> fluids = new HashSet<>();

    @SerializedName(value = "effects", alternate = {"effect", "status_effects", "status_effect", "mob_effects", "mob_effect"})
    public Set<String> effects = new HashSet<>();

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
    private transient volatile List<Pattern> compiledNbtRegex;
    private transient volatile List<CompoundTag> parsedNbtTags;
    private transient volatile Map<String, TagKey<Item>> compiledItemTags;
    private transient volatile Map<String, TagKey<Potion>> compiledPotionTags;
    private transient volatile Map<String, TagKey<Enchantment>> compiledEnchTags;

    public boolean matches(ItemStack stack, String itemId, String dimension, String entityId, Entity targetEntity, Holder<?> registryHolder, String context) {
        if (!matchesLogic(stack, itemId, dimension, entityId, targetEntity, this.action, registryHolder, context)) return false;

        if (nbt != null && !nbt.isEmpty()) {
            if (stack == null || stack.isEmpty() || !stack.hasTag()) return false;

            if (compiledNbtRegex == null || parsedNbtTags == null) {
                synchronized (this) {
                    if (compiledNbtRegex == null || parsedNbtTags == null) {
                        List<Pattern> regexList = new ArrayList<>();
                        List<CompoundTag> tagList = new ArrayList<>();

                        for (String ruleNbtStr : nbt) {
                            if (ruleNbtStr.startsWith("/") && ruleNbtStr.endsWith("/")) {
                                Pattern p = compile(ruleNbtStr);
                                if (p != null) regexList.add(p);
                            } else {
                                try {
                                    tagList.add(TagParser.parseTag(ruleNbtStr));
                                } catch (CommandSyntaxException e) {
                                    Constants.LOG.error("Reliable Remover: Invalid SNBT format in config: '{}'", ruleNbtStr);
                                }
                            }
                        }
                        compiledNbtRegex = regexList;
                        parsedNbtTags = tagList;
                    }
                }
            }

            CompoundTag itemTag = stack.getTag();

            for (CompoundTag requiredTag : parsedNbtTags) {
                if (NbtUtils.compareNbt(requiredTag, itemTag, true)) {
                    return true;
                }
            }

            if (!compiledNbtRegex.isEmpty()) {
                String dataStr = Objects.requireNonNull(itemTag).toString();
                for (Pattern p : compiledNbtRegex) {
                    if (p.matcher(dataStr).matches()) {
                        return true;
                    }
                }
            }

            return false;
        }

        return true;
    }

    private boolean matchesLogic(ItemStack stack, String itemId, String dimension, String entityId, Entity targetEntity, Action currentAction, Holder<?> registryHolder, String context) {
        if (currentAction == null) currentAction = Action.REMOVE;

        if (not != null && not.matchesLogic(stack, itemId, dimension, entityId, targetEntity, currentAction, registryHolder, context))
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
        boolean hasItemFilter = (items != null && !items.isEmpty()) || (blocks != null && !blocks.isEmpty()) || (fluids != null && !fluids.isEmpty()) || (effects != null && !effects.isEmpty()) || hasPattern || hasPatternList || hasTagFilter;
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

        if (blocks != null && !blocks.isEmpty()) {
            for (String filter : blocks) {
                if (filter.startsWith("#")) {
                    if (checkTag(filter, itemLocation, stack, currentAction, registryHolder)) return true;
                } else if (filter.equals(itemId)) {
                    return true;
                }
            }
        }

        if (fluids != null && !fluids.isEmpty()) {
            for (String filter : fluids) {
                if (filter.startsWith("#")) {
                    if (checkTag(filter, itemLocation, stack, currentAction, registryHolder)) return true;
                } else if (filter.equals(itemId)) {
                    return true;
                }
            }
        }

        if (effects != null && !effects.isEmpty()) {
            for (String filter : effects) {
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
                if (compiledPotionTags == null) {
                    synchronized (this) {
                        if (compiledPotionTags == null) compiledPotionTags = new ConcurrentHashMap<>();
                    }
                }
                TagKey<Potion> tagKey = compiledPotionTags.computeIfAbsent(cleanTagId, k -> TagKey.create(Registries.POTION, tagLocation));
                if (BuiltInRegistries.POTION.getHolder(ResourceKey.create(Registries.POTION, itemLocation))
                        .map(holder -> holder.is(tagKey))
                        .orElse(false)) {
                    return true;
                }
                TagKey<MobEffect> effectTagKey = TagKey.create(Registries.MOB_EFFECT, tagLocation);
                return BuiltInRegistries.MOB_EFFECT.getHolder(ResourceKey.create(Registries.MOB_EFFECT, itemLocation))
                        .map(holder -> holder.is(effectTagKey))
                        .orElse(false);

            } else if (currentAction == Action.REMOVE_EFFECT) {
                TagKey<MobEffect> effectTagKey = TagKey.create(Registries.MOB_EFFECT, tagLocation);
                return BuiltInRegistries.MOB_EFFECT.getHolder(ResourceKey.create(Registries.MOB_EFFECT, itemLocation))
                        .map(holder -> holder.is(effectTagKey))
                        .orElse(false);

            } else if (currentAction == Action.REMOVE_ENCHANTMENT) {
                if (registryHolder != null) {
                    if (compiledEnchTags == null) {
                        synchronized (this) {
                            if (compiledEnchTags == null) compiledEnchTags = new ConcurrentHashMap<>();
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
                        if (compiledItemTags == null) compiledItemTags = new ConcurrentHashMap<>();
                    }
                }
                TagKey<Item> tagKey = compiledItemTags.computeIfAbsent(cleanTagId, k -> TagKey.create(Registries.ITEM, tagLocation));
                if (stack != null && !stack.isEmpty()) {
                    if (stack.is(tagKey)) return true;
                }
                if (BuiltInRegistries.ITEM.getHolder(ResourceKey.create(Registries.ITEM, itemLocation))
                        .map(holder -> holder.is(tagKey))
                        .orElse(false)) {
                    return true;
                }
                TagKey<Block> blockTagKey = TagKey.create(Registries.BLOCK, tagLocation);
                if (BuiltInRegistries.BLOCK.getHolder(ResourceKey.create(Registries.BLOCK, itemLocation))
                        .map(holder -> holder.is(blockTagKey))
                        .orElse(false)) {
                    return true;
                }
                TagKey<Fluid> fluidTagKey = TagKey.create(Registries.FLUID, tagLocation);
                if (BuiltInRegistries.FLUID.getHolder(ResourceKey.create(Registries.FLUID, itemLocation))
                        .map(holder -> holder.is(fluidTagKey))
                        .orElse(false)) {
                    return true;
                }
                TagKey<MobEffect> effectTagKey = TagKey.create(Registries.MOB_EFFECT, tagLocation);
                return BuiltInRegistries.MOB_EFFECT.getHolder(ResourceKey.create(Registries.MOB_EFFECT, itemLocation))
                        .map(holder -> holder.is(effectTagKey))
                        .orElse(false);
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
            Constants.LOG.error("Invalid regex pattern found in config: '{}'. Skipping this pattern.", regex);
            return null;
        }
    }

    public void expandTags() {
        if (this.tags == null || this.tags.isEmpty()) return;

        Set<String> expandedItems = new HashSet<>(this.items);

        for (String tagId : this.tags) {
            String cleanTagId = tagId.startsWith("#") ? tagId.substring(1) : tagId;
            ResourceLocation tagLocation = ResourceLocation.tryParse(cleanTagId);
            if (tagLocation == null) continue;

            Set<String> resolved = new HashSet<>();
            if (this.action == Action.REMOVE_POTION) {
                TagKey<Potion> tagKey = TagKey.create(Registries.POTION, tagLocation);
                for (Map.Entry<ResourceKey<Potion>, Potion> entry : BuiltInRegistries.POTION.entrySet()) {
                    BuiltInRegistries.POTION.getHolder(entry.getKey()).ifPresent(holder -> {
                        if (holder.is(tagKey)) {
                            resolved.add(entry.getKey().location().toString());
                        }
                    });
                }
                TagKey<MobEffect> effectTagKey = TagKey.create(Registries.MOB_EFFECT, tagLocation);
                for (Map.Entry<ResourceKey<MobEffect>, MobEffect> entry : BuiltInRegistries.MOB_EFFECT.entrySet()) {
                    BuiltInRegistries.MOB_EFFECT.getHolder(entry.getKey()).ifPresent(holder -> {
                        if (holder.is(effectTagKey)) {
                            resolved.add(entry.getKey().location().toString());
                        }
                    });
                }
            } else if (this.action == Action.REMOVE_EFFECT) {
                TagKey<MobEffect> tagKey = TagKey.create(Registries.MOB_EFFECT, tagLocation);
                for (Map.Entry<ResourceKey<MobEffect>, MobEffect> entry : BuiltInRegistries.MOB_EFFECT.entrySet()) {
                    BuiltInRegistries.MOB_EFFECT.getHolder(entry.getKey()).ifPresent(holder -> {
                        if (holder.is(tagKey)) {
                            resolved.add(entry.getKey().location().toString());
                        }
                    });
                }
            } else if (this.action == Action.REMOVE_ENCHANTMENT) {
                TagKey<Enchantment> tagKey = TagKey.create(Registries.ENCHANTMENT, tagLocation);
                for (Map.Entry<ResourceKey<Enchantment>, Enchantment> entry : BuiltInRegistries.ENCHANTMENT.entrySet()) {
                    BuiltInRegistries.ENCHANTMENT.getHolder(entry.getKey()).ifPresent(holder -> {
                        if (holder.is(tagKey)) {
                            resolved.add(entry.getKey().location().toString());
                        }
                    });
                }
            } else {
                TagKey<Item> tagKey = TagKey.create(Registries.ITEM, tagLocation);
                for (Item item : BuiltInRegistries.ITEM) {
                    ResourceLocation key = BuiltInRegistries.ITEM.getKey(item);
                    BuiltInRegistries.ITEM.getHolder(ResourceKey.create(Registries.ITEM, key)).ifPresent(holder -> {
                        if (holder.is(tagKey)) {
                            resolved.add(key.toString());
                        }
                    });
                }
            }

            if (!resolved.isEmpty()) {
                RuleManager.EXPANDED_TAGS_CACHE.put(cleanTagId, resolved);
                expandedItems.addAll(resolved);
            } else {
                Set<String> cached = RuleManager.EXPANDED_TAGS_CACHE.get(cleanTagId);
                if (cached != null) {
                    expandedItems.addAll(cached);
                }
            }
        }

        this.items = expandedItems;
    }
}