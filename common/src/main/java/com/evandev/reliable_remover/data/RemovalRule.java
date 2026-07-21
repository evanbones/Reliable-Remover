package com.evandev.reliable_remover.data;

import com.evandev.reliable_remover.Constants;
import com.evandev.reliable_remover.config.AdvancementCache;
import com.evandev.reliable_remover.config.RuleManager;
import com.google.gson.annotations.SerializedName;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
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

    public Set<String> items = new HashSet<>();

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

    @SerializedName(value = "advancements", alternate = {"advancement"})
    public Set<String> advancements = new HashSet<>();

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

    /**
     * Returns true when the player has earned all listed advancements.
     */
    private static boolean checkAllAdvancements(Entity entity, Set<String> advancements) {
        for (String advId : advancements) {
            ResourceLocation loc = ResourceLocation.tryParse(advId);
            if (loc == null) continue;

            if (entity instanceof ServerPlayer serverPlayer) {
                MinecraftServer server = serverPlayer.getServer();
                if (server == null) return false;
                AdvancementHolder holder = server.getAdvancements().get(loc);
                if (holder == null) return false;
                AdvancementProgress progress = serverPlayer.getAdvancements().getOrStartProgress(holder);
                if (!progress.isDone()) return false;
            } else {
                if (!AdvancementCache.isDone(loc)) return false;
            }
        }
        return true;
    }

    public boolean matches(ItemStack stack, String itemId, String dimension, String entityId, Entity entity, Holder<?> registryHolder, String context) {
        if (!matchesLogic(stack, itemId, dimension, entityId, entity, this.action, registryHolder, context))
            return false;

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
                    ? stack.get(DataComponents.CUSTOM_DATA).getUnsafe().toString()
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

    private boolean matchesLogic(ItemStack stack, String itemId, String dimension, String entityId, Entity entity, Action currentAction, Holder<?> registryHolder, String context) {
        if (currentAction == null) currentAction = Action.REMOVE;

        if (not != null && not.matchesLogic(stack, itemId, dimension, entityId, entity, currentAction, registryHolder, context))
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

        if (advancements != null && !advancements.isEmpty()) {
            if (RuleManager.isSkippingAdvancementCheck() || checkAllAdvancements(entity, advancements)) return false;
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
        boolean hasItemFilter = (items != null && !items.isEmpty()) || (blocks != null && !blocks.isEmpty()) || (fluids != null && !fluids.isEmpty()) || (effects != null && !effects.isEmpty()) || hasPattern || hasPatternList || hasTagFilter;
        boolean hasModFilter = (mod != null && !mod.isEmpty());

        if (!hasItemFilter && !hasModFilter) {
            return (dimensions != null && !dimensions.isEmpty()) ||
                    (entities != null && !entities.isEmpty()) ||
                    hasNbtFilter ||
                    (advancements != null && !advancements.isEmpty());
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
                if (registryHolder != null && registryHolder.value() instanceof MobEffect) {
                    @SuppressWarnings("unchecked")
                    Holder<MobEffect> effectHolder = (Holder<MobEffect>) registryHolder;
                    if (effectHolder.is(effectTagKey)) return true;
                }
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
            Constants.LOG.error("Reliable Remover: Invalid regex pattern found in config: '{}'. Skipping this pattern.", regex);
            return null;
        }
    }

    public void expandTags(RegistryAccess registryAccess) {
        if (this.tags == null || this.tags.isEmpty()) return;

        for (String tagId : this.tags) {
            String cleanTagId = tagId.startsWith("#") ? tagId.substring(1) : tagId;
            ResourceLocation tagLocation = ResourceLocation.tryParse(cleanTagId);
            if (tagLocation == null) continue;

            Set<String> resolved = new HashSet<>();
            if (this.action == Action.REMOVE_POTION) {
                TagKey<Potion> tagKey = TagKey.create(Registries.POTION, tagLocation);
                registryAccess.registry(Registries.POTION).ifPresent(registry -> {
                    for (Map.Entry<ResourceKey<Potion>, Potion> entry : registry.entrySet()) {
                        registry.getHolder(entry.getKey()).ifPresent(holder -> {
                            if (holder.is(tagKey)) {
                                resolved.add(entry.getKey().location().toString());
                            }
                        });
                    }
                });
            } else if (this.action == Action.REMOVE_ENCHANTMENT) {
                TagKey<Enchantment> tagKey = TagKey.create(Registries.ENCHANTMENT, tagLocation);
                registryAccess.registry(Registries.ENCHANTMENT).ifPresent(registry -> {
                    for (Map.Entry<ResourceKey<Enchantment>, Enchantment> entry : registry.entrySet()) {
                        registry.getHolder(entry.getKey()).ifPresent(holder -> {
                            if (holder.is(tagKey)) {
                                resolved.add(entry.getKey().location().toString());
                            }
                        });
                    }
                });
            } else {
                TagKey<Item> tagKey = TagKey.create(Registries.ITEM, tagLocation);
                registryAccess.registry(Registries.ITEM).ifPresent(registry -> {
                    for (Item item : registry) {
                        ResourceLocation key = registry.getKey(item);
                        if (key == null) continue;
                        registry.getHolder(ResourceKey.create(Registries.ITEM, key)).ifPresent(holder -> {
                            if (holder.is(tagKey)) {
                                resolved.add(key.toString());
                            }
                        });
                    }
                });
            }

            if (!resolved.isEmpty()) {
                RuleManager.EXPANDED_TAGS_CACHE.put(cleanTagId, resolved);
                this.items.addAll(resolved);
            } else {
                Set<String> cached = RuleManager.EXPANDED_TAGS_CACHE.get(cleanTagId);
                if (cached != null) {
                    this.items.addAll(cached);
                }
            }
        }
    }
}
