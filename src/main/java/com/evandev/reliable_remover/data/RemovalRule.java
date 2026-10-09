package com.evandev.reliable_remover.data;

import com.evandev.reliable_remover.Constants;
import com.evandev.reliable_remover.config.RuleManager;
import com.google.gson.annotations.SerializedName;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.arguments.item.ItemParser;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
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
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

//? if >=1.21 {
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.arguments.item.ItemPredicateArgument;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.flag.FeatureFlags;
//?}
//? if >=1.21 {
import net.minecraft.world.item.enchantment.ItemEnchantments;
//?} else {
/*import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
*///?}

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

    @SerializedName(value = "replace_components", alternate = {"replacement_components", "replace_nbt", "replacement_nbt"})
    public String replaceComponents;

    public RemovalRule not;

    private transient volatile List<Pattern> compiledPatterns;
    private transient volatile List<Predicate<ItemStack>> compiledNbtMatchers;
    private transient volatile Map<String, TagKey<Item>> compiledItemTags;
    private transient volatile Map<String, TagKey<Potion>> compiledPotionTags;
    private transient volatile Map<String, TagKey<Enchantment>> compiledEnchTags;
    private transient volatile ParsedReplacement compiledReplacement;
    private transient volatile boolean replacementInvalid;

    //? if >=1.21 {
    private record ParsedReplacement(Item item, DataComponentPatch components) {
    }
    //?} else {
    /*private record ParsedReplacement(Item item, CompoundTag nbt) {
    }
    *///?}

    public boolean matches(ItemStack stack, String itemId, String dimension, String entityId, Entity targetEntity, Holder<?> registryHolder, String context) {
        if (!matchesLogic(stack, itemId, dimension, entityId, targetEntity, this.action, registryHolder, context))
            return false;

        if (nbt != null && !nbt.isEmpty()) {
            if (stack == null || stack.isEmpty()) return false;

            List<Predicate<ItemStack>> matchers = getNbtMatchers();
            if (matchers == null) return false;

            for (Predicate<ItemStack> matcher : matchers) {
                if (matcher.test(stack)) return true;
            }
            return false;
        }

        return true;
    }

    private List<Predicate<ItemStack>> getNbtMatchers() {
        if (compiledNbtMatchers == null) {
            synchronized (this) {
                if (compiledNbtMatchers == null) {
                    //? if >=1.21 {
                    HolderLookup.Provider registries = RuleManager.getRegistries();
                    if (registries == null) return null;
                    CommandBuildContext buildContext = CommandBuildContext.simple(registries, FeatureFlags.REGISTRY.allFlags());
                    //?}
                    List<Predicate<ItemStack>> list = new ArrayList<>();
                    for (String entry : nbt) {
                        //? if >=1.21 {
                        Predicate<ItemStack> matcher = compileNbtMatcher(entry, buildContext);
                        //?} else {
                        /*Predicate<ItemStack> matcher = compileNbtMatcher(entry);
                         *///?}
                        if (matcher != null) list.add(matcher);
                    }
                    compiledNbtMatchers = list;
                }
            }
        }
        return compiledNbtMatchers;
    }

    //? if >=1.21 {
    private Predicate<ItemStack> compileNbtMatcher(String entry, CommandBuildContext buildContext) {
        String trimmed = entry.trim();
        if (isSlashRegex(trimmed)) return componentRegexMatcher(entry, compileQuietly(trimmed));

        if (trimmed.startsWith("{")) {
            Constants.LOG.error("Reliable Remover: SNBT filter '{}' is not supported on Minecraft 1.21+. Use item component syntax instead, e.g. '[custom_data~{key:value}]'.", entry);
            return null;
        }

        String predicate = trimmed.startsWith("[") ? "*" + trimmed : trimmed;
        try {
            StringReader reader = new StringReader(predicate);
            ItemPredicateArgument.Result result = new ItemPredicateArgument(buildContext).parse(reader);
            if (!reader.canRead()) return result;
        } catch (CommandSyntaxException ignored) {
        }

        Pattern legacyRegex = compileQuietly(trimmed);
        if (legacyRegex == null) {
            Constants.LOG.error("Reliable Remover: Invalid component filter '{}'. Expected item component syntax (e.g. '[damage=0]') or a /regex/.", entry);
            return null;
        }
        return componentRegexMatcher(entry, legacyRegex);
    }

    private static Predicate<ItemStack> componentRegexMatcher(String entry, Pattern pattern) {
        if (pattern == null) {
            Constants.LOG.error("Reliable Remover: Invalid regex pattern found in config: '{}'. Skipping this pattern.", entry);
            return null;
        }
        return stack -> {
            if (pattern.matcher(stack.getComponents().toString()).matches()) return true;
            var customData = stack.get(DataComponents.CUSTOM_DATA);
            return customData != null && pattern.matcher(customData.copyTag().toString()).matches();
        };
    }
    //?} else {
    /*
    private Predicate<ItemStack> compileNbtMatcher(String entry) {
        String trimmed = entry.trim();
        if (!isSlashRegex(trimmed)) {
            try {
                CompoundTag required = TagParser.parseTag(trimmed);
                return stack -> stack.getTag() != null && NbtUtils.compareNbt(required, stack.getTag(), true);
            } catch (CommandSyntaxException ignored) {
            }
        }

        Pattern pattern = compileQuietly(trimmed);
        if (pattern == null) {
            Constants.LOG.error("Reliable Remover: Invalid NBT filter '{}'. Expected SNBT (e.g. '{Damage:0}') or a /regex/.", entry);
            return null;
        }
        return stack -> stack.getTag() != null && pattern.matcher(stack.getTag().toString()).matches();
    }
    *///?}

    public boolean hasReplacement() {
        return replaceWith != null && !replaceWith.isBlank();
    }

    /**
     * The item ID portion of {@code replace_with}, without any inline components/NBT.
     */
    public String getReplacementItemId() {
        if (!hasReplacement()) return null;
        String value = replaceWith.trim();
        int end = value.length();
        int bracket = value.indexOf('[');
        int brace = value.indexOf('{');
        if (bracket >= 0) end = bracket;
        if (brace >= 0) end = Math.min(end, brace);
        return value.substring(0, end).trim();
    }

    /**
     * Builds the replacement for {@code original}: the {@code replace_with} item carrying over the original's
     * data, with the rule's components ({@code replace_with} suffix and/or {@code replace_components}) applied on top.
     */
    public ItemStack createReplacement(ItemStack original) {
        ParsedReplacement parsed = getParsedReplacement();
        if (parsed == null) return null;
        ItemStack replacement = new ItemStack(parsed.item(), original.getCount());
        //? if >=1.21 {
        replacement.applyComponents(original.getComponentsPatch());
        replacement.applyComponents(parsed.components());
        //?} else {
        /*if (original.getTag() != null) replacement.setTag(original.getTag().copy());
        if (parsed.nbt() != null) replacement.getOrCreateTag().merge(parsed.nbt().copy());
        *///?}
        return replacement;
    }

    //? if <1.19.3 {
    /*private static net.minecraft.core.HolderLookup<Item> itemLookup() {
        return new net.minecraft.core.HolderLookup.RegistryLookup<>(BuiltInRegistries.ITEM);
    }

    *///?} else if <1.21 {
    /*private static HolderLookup<Item> itemLookup() {
        return BuiltInRegistries.ITEM.asLookup();
    }

    *///?}
    private ParsedReplacement getParsedReplacement() {
        if (!hasReplacement() || replacementInvalid) return null;
        ParsedReplacement cached = compiledReplacement;
        if (cached != null) return cached;

        String itemId = getReplacementItemId();
        String input = replaceWith.trim() + (replaceComponents != null ? replaceComponents.trim() : "");
        if (input.length() == itemId.length()) {
            Identifier id = Identifier.tryParse(itemId);
            Item item = id != null ? BuiltInRegistries.ITEM.getOptional(id).orElse(null) : null;
            if (item == null) return null;
            //? if >=1.21 {
            return compiledReplacement = new ParsedReplacement(item, DataComponentPatch.EMPTY);
            //?} else {
            /*return compiledReplacement = new ParsedReplacement(item, null);
            *///?}
        }

        //? if >=1.21 {
        HolderLookup.Provider registries = RuleManager.getRegistries();
        if (registries == null) {
            Identifier id = Identifier.tryParse(itemId);
            Item item = id != null ? BuiltInRegistries.ITEM.getOptional(id).orElse(null) : null;
            return item != null ? new ParsedReplacement(item, DataComponentPatch.EMPTY) : null;
        }
        //?}
        try {
            StringReader reader = new StringReader(input);
            //? if >=1.21 {
            AtomicReference<Holder<Item>> item = new AtomicReference<>();
            DataComponentPatch.Builder components = DataComponentPatch.builder();
            new ItemParser(registries).parse(reader, new ItemParser.Visitor() {
                @Override
                public void visitItem(Holder<Item> holder) {
                    item.set(holder);
                }

                @Override
                public <T> void visitComponent(DataComponentType<T> type, T value) {
                    components.set(type, value);
                }

                @Override
                public <T> void visitRemovedComponent(DataComponentType<T> type) {
                    components.remove(type);
                }
            });
            ParsedReplacement parsed = new ParsedReplacement(item.get().value(), components.build());
            //?} else {
            /*ItemParser.ItemResult result = ItemParser.parseForItem(itemLookup(), reader);
            ParsedReplacement parsed = new ParsedReplacement(result.item().value(), result.nbt());
            *///?}
            if (reader.canRead()) {
                Constants.LOG.error("Reliable Remover: Unexpected trailing input in replacement '{}' at position {}.", input, reader.getCursor());
                replacementInvalid = true;
                return null;
            }
            return compiledReplacement = parsed;
        } catch (CommandSyntaxException e) {
            Constants.LOG.error("Reliable Remover: Invalid replacement '{}': {}", input, e.getMessage());
            replacementInvalid = true;
            return null;
        }
    }

    private static boolean isSlashRegex(String value) {
        return value.length() > 1 && value.startsWith("/") && value.endsWith("/");
    }

    private static Pattern compileQuietly(String regex) {
        String p = isSlashRegex(regex) ? regex.substring(1, regex.length() - 1) : regex;
        try {
            return Pattern.compile(p);
        } catch (PatternSyntaxException e) {
            return null;
        }
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

        //? if >=1.21 {
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
        //?} else {
        /*CompoundTag tag = stack.getTag();
        if (tag == null) return false;
        for (String key : List.of("Enchantments", "StoredEnchantments")) {
            ListTag list = tag.getList(key, Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                Identifier enchId = EnchantmentHelper.getEnchantmentId(list.getCompound(i));
                if (enchId == null) continue;
                Holder<Enchantment> holder = BuiltInRegistries.ENCHANTMENT.getHolder(net.minecraft.resources.ResourceKey.create(Registries.ENCHANTMENT, enchId)).orElse(null);
                if (matchesEnchantmentHolder(holder)) return true;
            }
        }
        *///?}

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

    public static <T> Optional<Holder.Reference<T>> getRegistryHolder(Registry<T> registry, Identifier id) {
        //? if >=26.1 {
        return registry.get(id);
        //?} else if >=1.21 {
        /*return registry.getHolder(id);
         *///?} else if >=1.19.3 {
        /*return registry.getHolder(net.minecraft.resources.ResourceKey.create(registry.key(), id));
         *///?} else {
        /*return registry.getHolder(net.minecraft.resources.ResourceKey.create(registry.key(), id)).map(holder -> (Holder.Reference<T>) holder);
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

        this.compiledNbtMatchers = null;

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