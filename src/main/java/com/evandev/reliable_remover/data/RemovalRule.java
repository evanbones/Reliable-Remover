package com.evandev.reliable_remover.data;

import com.evandev.reliable_remover.Constants;
import com.evandev.reliable_remover.config.RuleManager;
import com.google.gson.annotations.SerializedName;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
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

    @SerializedName(value = "items", alternate = {"item", "potion", "potions", "mob", "mobs", "mob_equipment"})
    public volatile Set<String> items = new HashSet<>();

    @SerializedName(value = "enchantments", alternate = {"enchantment"})
    public Set<String> enchantments = new HashSet<>();

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
    private transient volatile List<Pattern> compiledComponentRegex;
    private transient volatile Map<String, TagKey<Item>> compiledItemTags;
    private transient volatile Map<String, TagKey<Potion>> compiledPotionTags;
    private transient volatile Map<String, TagKey<Enchantment>> compiledEnchTags;

    public boolean matches(ItemStack stack, String itemId, String dimension, String entityId, Holder<?> registryHolder, String context) {
        return matches(stack, itemId, dimension, entityId, null, registryHolder, context);
    }

    public boolean matches(ItemStack stack, String itemId, String dimension, String entityId, Entity targetEntity, Holder<?> registryHolder, String context) {
        if (!matchesLogic(stack, itemId, dimension, entityId, targetEntity, this.action, registryHolder, context)) return false;

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

    private boolean matchesItem(String filter, ItemStack stack, String stackItemId) {
        if (filter.startsWith("#")) {
            String cleanTagId = filter.substring(1);
            Identifier tagLocation = Identifier.tryParse(cleanTagId);
            if (tagLocation == null) return false;
            if (compiledItemTags == null) {
                synchronized (this) {
                    if (compiledItemTags == null) compiledItemTags = new ConcurrentHashMap<>();
                }
            }
            TagKey<Item> tagKey = compiledItemTags.computeIfAbsent(cleanTagId, k -> TagKey.create(Registries.ITEM, tagLocation));
            return stack != null && !stack.isEmpty() && stack.is(tagKey);
        } else {
            return filter.equals(stackItemId);
        }
    }

    private boolean matchesAnyItem(Set<String> itemFilters, ItemStack stack) {
        if (itemFilters == null || itemFilters.isEmpty() || stack == null || stack.isEmpty()) return false;
        String stackItemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        for (String filter : itemFilters) {
            if (matchesItem(filter, stack, stackItemId)) return true;
        }
        return false;
    }

    public boolean matchesAnyEnchantment(ItemStack stack) {
        if (this.enchantments == null || this.enchantments.isEmpty() || stack == null || stack.isEmpty()) return false;

        ItemEnchantments enchs = stack.get(DataComponents.ENCHANTMENTS);
        if (enchs != null && !enchs.isEmpty()) {
            for (Holder<Enchantment> holder : enchs.keySet()) {
                if (matchesEnchantmentHolder(holder)) return true;
            }
        }

        ItemEnchantments stored = stack.get(DataComponents.STORED_ENCHANTMENTS);
        if (stored != null && !stored.isEmpty()) {
            for (Holder<Enchantment> holder : stored.keySet()) {
                if (matchesEnchantmentHolder(holder)) return true;
            }
        }

        return false;
    }

    private boolean matchesEnchantmentHolder(Holder<Enchantment> holder) {
        if (holder == null) return false;
        String id = holder.unwrapKey().map(k -> k.identifier().toString()).orElse(null);

        for (String filter : this.enchantments) {
            if (filter.startsWith("#")) {
                String cleanTagId = filter.substring(1);
                Identifier tagLocation = Identifier.tryParse(cleanTagId);
                if (tagLocation != null) {
                    if (compiledEnchTags == null) {
                        synchronized (this) {
                            if (compiledEnchTags == null) compiledEnchTags = new ConcurrentHashMap<>();
                        }
                    }
                    TagKey<Enchantment> tagKey = compiledEnchTags.computeIfAbsent(cleanTagId, k -> TagKey.create(Registries.ENCHANTMENT, tagLocation));
                    if (holder.is(tagKey)) return true;
                }
            } else if (id != null) {
                if (filter.equals(id)) return true;
                Identifier filterLoc = Identifier.tryParse(filter);
                if (filterLoc != null && filterLoc.toString().equals(id)) return true;
            }
        }
        return false;
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

        if (this.action == Action.REMOVE_INTERACTIONS && "interaction".equals(context)) {
            if (this.blocks != null && !this.blocks.isEmpty() && (this.items == null || this.items.isEmpty())) {
                return false;
            }
        }

        boolean hasPattern = pattern != null && !pattern.isEmpty();
        boolean hasPatternList = patterns != null && !patterns.isEmpty();
        boolean hasTagFilter = tags != null && !tags.isEmpty();
        boolean hasNbtFilter = nbt != null && !nbt.isEmpty();
        boolean hasEnchFilter = enchantments != null && !enchantments.isEmpty();
        boolean hasItemFilter = (items != null && !items.isEmpty()) || (blocks != null && !blocks.isEmpty()) || (fluids != null && !fluids.isEmpty()) || (effects != null && !effects.isEmpty()) || hasEnchFilter || hasPattern || hasPatternList || hasTagFilter;
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

        if (currentAction == Action.REMOVE_ENCHANTMENT) {
            boolean hasEnchExplicit = this.enchantments != null && !this.enchantments.isEmpty();
            boolean hasItemExplicit = this.items != null && !this.items.isEmpty();

            if (hasEnchExplicit) {
                boolean enchMatches = false;
                for (String filter : this.enchantments) {
                    if (filter.startsWith("#")) {
                        if (checkTag(filter, itemLocation, stack, currentAction, registryHolder)) {
                            enchMatches = true;
                            break;
                        }
                    } else if (filter.equals(itemId)) {
                        enchMatches = true;
                        break;
                    }
                }
                if (!enchMatches) return false;

                if (hasItemExplicit) {
                    if (stack == null || stack.isEmpty()) return false;
                    return matchesAnyItem(this.items, stack);
                }
                return true;
            }

            if (hasTagFilter) {
                boolean tagMatches = false;
                for (String tagId : tags) {
                    if (checkTag(tagId, itemLocation, stack, currentAction, registryHolder)) {
                        tagMatches = true;
                        break;
                    }
                }
                if (tagMatches) {
                    if (hasItemExplicit) {
                        if (stack == null || stack.isEmpty()) return false;
                        return matchesAnyItem(this.items, stack);
                    }
                    return true;
                }
            }

            if (hasPattern || hasPatternList) {
                boolean patternMatches = false;
                for (Pattern p : getCompiledPatterns(hasPattern, hasPatternList)) {
                    if (p != null && p.matcher(itemId).matches()) {
                        patternMatches = true;
                        break;
                    }
                }
                if (patternMatches) {
                    if (hasItemExplicit) {
                        if (stack == null || stack.isEmpty()) return false;
                        return matchesAnyItem(this.items, stack);
                    }
                    return true;
                }
            }

            if (hasItemExplicit) {
                if (matchesAnyItem(this.items, stack)) return true;

                if (this.items.contains(itemId)) return true;
                for (String filter : this.items) {
                    if (filter.startsWith("#") && checkTag(filter, itemLocation, stack, currentAction, registryHolder)) {
                        return true;
                    }
                }
            }

            return false;
        }

        if (hasEnchFilter) {
            if (!matchesAnyEnchantment(stack)) return false;
            boolean hasOtherItemFilter = (items != null && !items.isEmpty()) ||
                    (blocks != null && !blocks.isEmpty()) ||
                    (fluids != null && !fluids.isEmpty()) ||
                    (effects != null && !effects.isEmpty()) ||
                    hasTagFilter ||
                    hasPattern ||
                    hasPatternList;
            if (!hasOtherItemFilter) return true;
        }

        for (Set<String> idFilters : Arrays.asList(items, blocks, fluids, effects)) {
            if (idFilters == null || idFilters.isEmpty()) continue;
            for (String filter : idFilters) {
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
            for (Pattern p : getCompiledPatterns(hasPattern, hasPatternList)) {
                if (p != null && p.matcher(itemId).matches()) return true;
            }
        }

        return false;
    }

    private List<Pattern> getCompiledPatterns(boolean hasPattern, boolean hasPatternList) {
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
        return compiledPatterns;
    }

    private static <T> Optional<Holder.Reference<T>> getRegistryHolder(Registry<T> registry, Identifier id) {
        //? if >=26.1 {
        return registry.get(id);
        //?} else {
        /*return registry.getHolder(id);
        *///?}
    }

    private boolean checkTag(String tagId, Identifier itemLocation, ItemStack stack, Action currentAction, Holder<?> registryHolder) {
        String cleanTagId = tagId.startsWith("#") ? tagId.substring(1) : tagId;
        Identifier tagLocation = Identifier.tryParse(cleanTagId);

        if (tagLocation != null && itemLocation != null) {
            if (currentAction == Action.REMOVE_POTION) {
                if (compiledPotionTags == null) {
                    synchronized (this) {
                        if (compiledPotionTags == null) compiledPotionTags = new ConcurrentHashMap<>();
                    }
                }
                TagKey<Potion> tagKey = compiledPotionTags.computeIfAbsent(cleanTagId, k -> TagKey.create(Registries.POTION, tagLocation));
                if (getRegistryHolder(BuiltInRegistries.POTION, itemLocation)
                        .map(holder -> holder.is(tagKey))
                        .orElse(false)) {
                    return true;
                }
                return matchesEffectTag(tagLocation, itemLocation, registryHolder);

            } else if (currentAction == Action.REMOVE_EFFECT) {
                return matchesEffectTag(tagLocation, itemLocation, registryHolder);

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
                Set<String> cachedResolved = RuleManager.EXPANDED_TAGS_CACHE.get(cleanTagId);
                if (cachedResolved != null && cachedResolved.contains(itemLocation.toString())) {
                    return true;
                }

                if (registryHolder != null) {
                    if (registryHolder.value() instanceof Block) {
                        TagKey<Block> blockTagKey = TagKey.create(Registries.BLOCK, tagLocation);
                        @SuppressWarnings("unchecked")
                        Holder<Block> blockHolder = (Holder<Block>) registryHolder;
                        if (blockHolder.is(blockTagKey)) return true;
                    } else if (registryHolder.value() instanceof MobEffect) {
                        TagKey<MobEffect> effectTagKey = TagKey.create(Registries.MOB_EFFECT, tagLocation);
                        @SuppressWarnings("unchecked")
                        Holder<MobEffect> effectHolder = (Holder<MobEffect>) registryHolder;
                        if (effectHolder.is(effectTagKey)) return true;
                    } else if (registryHolder.value() instanceof Fluid) {
                        TagKey<Fluid> fluidTagKey = TagKey.create(Registries.FLUID, tagLocation);
                        @SuppressWarnings("unchecked")
                        Holder<Fluid> fluidHolder = (Holder<Fluid>) registryHolder;
                        if (fluidHolder.is(fluidTagKey)) return true;
                    }
                }
                if (compiledItemTags == null) {
                    synchronized (this) {
                        if (compiledItemTags == null) compiledItemTags = new ConcurrentHashMap<>();
                    }
                }
                TagKey<Item> tagKey = compiledItemTags.computeIfAbsent(cleanTagId, k -> TagKey.create(Registries.ITEM, tagLocation));
                if (stack != null && !stack.isEmpty()) {
                    if (stack.is(tagKey)) return true;
                }
                if (getRegistryHolder(BuiltInRegistries.ITEM, itemLocation)
                        .map(holder -> holder.is(tagKey))
                        .orElse(false)) {
                    return true;
                }
                TagKey<Block> blockTagKey = TagKey.create(Registries.BLOCK, tagLocation);
                if (getRegistryHolder(BuiltInRegistries.BLOCK, itemLocation)
                        .map(holder -> holder.is(blockTagKey))
                        .orElse(false)) {
                    return true;
                }
                TagKey<Fluid> fluidTagKey = TagKey.create(Registries.FLUID, tagLocation);
                if (getRegistryHolder(BuiltInRegistries.FLUID, itemLocation)
                        .map(holder -> holder.is(fluidTagKey))
                        .orElse(false)) {
                    return true;
                }
                TagKey<MobEffect> effectTagKey = TagKey.create(Registries.MOB_EFFECT, tagLocation);
                return getRegistryHolder(BuiltInRegistries.MOB_EFFECT, itemLocation)
                        .map(holder -> holder.is(effectTagKey))
                        .orElse(false);
            }
        }
        return false;
    }

    private static boolean matchesEffectTag(Identifier tagLocation, Identifier itemLocation, Holder<?> registryHolder) {
        TagKey<MobEffect> effectTagKey = TagKey.create(Registries.MOB_EFFECT, tagLocation);
        if (registryHolder != null && registryHolder.value() instanceof MobEffect) {
            @SuppressWarnings("unchecked")
            Holder<MobEffect> effectHolder = (Holder<MobEffect>) registryHolder;
            if (effectHolder.is(effectTagKey)) return true;
        }
        return getRegistryHolder(BuiltInRegistries.MOB_EFFECT, itemLocation)
                .map(holder -> holder.is(effectTagKey))
                .orElse(false);
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

    public void expandTags(HolderLookup.Provider registries) {
        if (this.not != null) {
            this.not.expandTags(registries);
        }

        if (this.tags == null || this.tags.isEmpty()) return;

        boolean enchantmentRule = this.action == Action.REMOVE_ENCHANTMENT;
        Set<String> expandedItems = new HashSet<>(enchantmentRule ? this.enchantments : this.items);

        for (String tagId : this.tags) {
            String cleanTagId = tagId.startsWith("#") ? tagId.substring(1) : tagId;
            Identifier tagLocation = Identifier.tryParse(cleanTagId);
            if (tagLocation == null) continue;

            Set<String> resolved = new HashSet<>();
            if (this.action == Action.REMOVE_POTION) {
                TagKey<Potion> tagKey = TagKey.create(Registries.POTION, tagLocation);
                registries.lookup(Registries.POTION).ifPresent(lookup -> {
                    lookup.listElements().forEach(holder -> {
                        if (holder.is(tagKey)) {
                            resolved.add(holder.key().identifier().toString());
                        }
                    });
                });
                TagKey<MobEffect> effectTagKey = TagKey.create(Registries.MOB_EFFECT, tagLocation);
                registries.lookup(Registries.MOB_EFFECT).ifPresent(lookup -> {
                    lookup.listElements().forEach(holder -> {
                        if (holder.is(effectTagKey)) {
                            resolved.add(holder.key().identifier().toString());
                        }
                    });
                });
            } else if (this.action == Action.REMOVE_EFFECT) {
                TagKey<MobEffect> tagKey = TagKey.create(Registries.MOB_EFFECT, tagLocation);
                registries.lookup(Registries.MOB_EFFECT).ifPresent(lookup -> {
                    lookup.listElements().forEach(holder -> {
                        if (holder.is(tagKey)) {
                            resolved.add(holder.key().identifier().toString());
                        }
                    });
                });
            } else if (this.action == Action.REMOVE_ENCHANTMENT) {
                TagKey<Enchantment> tagKey = TagKey.create(Registries.ENCHANTMENT, tagLocation);
                registries.lookup(Registries.ENCHANTMENT).ifPresent(lookup -> {
                    lookup.listElements().forEach(holder -> {
                        if (holder.is(tagKey)) {
                            resolved.add(holder.key().identifier().toString());
                        }
                    });
                });
            } else {
                TagKey<Item> tagKey = TagKey.create(Registries.ITEM, tagLocation);
                registries.lookup(Registries.ITEM).ifPresent(lookup -> {
                    lookup.listElements().forEach(holder -> {
                        if (holder.is(tagKey)) {
                            resolved.add(holder.key().identifier().toString());
                        }
                    });
                });
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

        if (enchantmentRule) {
            this.enchantments = expandedItems;
        } else {
            this.items = expandedItems;
        }
    }
}