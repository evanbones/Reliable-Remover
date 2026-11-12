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
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.Entity;
//? if >=26.1 {
import net.minecraft.world.entity.decoration.painting.PaintingVariant;
//?} else {
/*import net.minecraft.world.entity.decoration.PaintingVariant;
*///?}
import net.minecraft.world.item.BlockItem;
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

    @SerializedName(value = "items", alternate = {"item", "potion", "potions", "mob", "mobs", "mob_equipment", "painting", "paintings"})
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
    private transient volatile Map<String, TagKey<?>> compiledTags;
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
            return stack != null && !stack.isEmpty() && stack.is(cachedTag(Registries.ITEM, cleanTagId, tagLocation));
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
                if (tagLocation != null && holder.is(cachedTag(Registries.ENCHANTMENT, cleanTagId, tagLocation))) return true;
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

        boolean hasPattern = notEmpty(pattern);
        boolean hasPatternList = notEmpty(patterns);
        boolean hasTagFilter = notEmpty(tags);
        boolean hasNbtFilter = notEmpty(nbt);
        boolean hasEnchFilter = notEmpty(enchantments);
        boolean hasIdFilter = notEmpty(items) || notEmpty(blocks) || notEmpty(fluids) || notEmpty(effects);
        boolean hasItemFilter = hasIdFilter || hasEnchFilter || hasPattern || hasPatternList || hasTagFilter;
        boolean hasModFilter = notEmpty(mod);

        if (!hasItemFilter && !hasModFilter) {
            return notEmpty(dimensions) || notEmpty(entities) || hasNbtFilter;
        }

        if (hasModFilter) {
            String[] split = itemId.split(":");
            if (split.length < 2 || !mod.contains(split[0])) return false;
            if (!hasItemFilter) return true;
        }

        Identifier itemLocation = Identifier.tryParse(itemId);

        if (currentAction == Action.REMOVE_ENCHANTMENT) {
            return matchesEnchantmentRule(stack, itemId, itemLocation, registryHolder, hasTagFilter, hasPattern, hasPatternList);
        }

        if (hasEnchFilter) {
            if (!matchesAnyEnchantment(stack)) return false;
            if (!hasIdFilter && !hasTagFilter && !hasPattern && !hasPatternList) return true;
        }

        for (Set<String> idFilters : Arrays.asList(items, blocks, fluids, effects)) {
            if (idFilters != null && anyIdOrTagMatches(idFilters, itemId, itemLocation, stack, currentAction, registryHolder)) return true;
        }
        if (hasTagFilter && anyTagMatches(tags, itemLocation, stack, currentAction, registryHolder)) return true;
        return (hasPattern || hasPatternList) && anyPatternMatches(itemId, hasPattern, hasPatternList);
    }

    private boolean matchesEnchantmentRule(ItemStack stack, String itemId, Identifier itemLocation, Holder<?> registryHolder, boolean hasTagFilter, boolean hasPattern, boolean hasPatternList) {
        boolean hasItemFilter = notEmpty(items);

        if (notEmpty(enchantments)) {
            if (!anyIdOrTagMatches(enchantments, itemId, itemLocation, stack, Action.REMOVE_ENCHANTMENT, registryHolder)) return false;
            return !hasItemFilter || stackMatchesAnyItem(stack);
        }
        if (hasTagFilter && anyTagMatches(tags, itemLocation, stack, Action.REMOVE_ENCHANTMENT, registryHolder)) {
            return !hasItemFilter || stackMatchesAnyItem(stack);
        }
        if ((hasPattern || hasPatternList) && anyPatternMatches(itemId, hasPattern, hasPatternList)) {
            return !hasItemFilter || stackMatchesAnyItem(stack);
        }
        if (hasItemFilter) {
            if (matchesAnyItem(items, stack)) return true;
            if (items.contains(itemId)) return true;
            for (String filter : items) {
                if (filter.startsWith("#") && checkTag(filter, itemLocation, stack, Action.REMOVE_ENCHANTMENT, registryHolder)) return true;
            }
        }
        return false;
    }

    private boolean stackMatchesAnyItem(ItemStack stack) {
        return stack != null && !stack.isEmpty() && matchesAnyItem(items, stack);
    }

    private boolean anyIdOrTagMatches(Collection<String> filters, String itemId, Identifier itemLocation, ItemStack stack, Action currentAction, Holder<?> registryHolder) {
        for (String filter : filters) {
            if (filter.startsWith("#")) {
                if (checkTag(filter, itemLocation, stack, currentAction, registryHolder)) return true;
            } else if (filter.equals(itemId)) {
                return true;
            }
        }
        return false;
    }

    private boolean anyTagMatches(Collection<String> tagIds, Identifier itemLocation, ItemStack stack, Action currentAction, Holder<?> registryHolder) {
        for (String tagId : tagIds) {
            if (checkTag(tagId, itemLocation, stack, currentAction, registryHolder)) return true;
        }
        return false;
    }

    private boolean anyPatternMatches(String itemId, boolean hasPattern, boolean hasPatternList) {
        for (Pattern p : getCompiledPatterns(hasPattern, hasPatternList)) {
            if (p != null && p.matcher(itemId).matches()) return true;
        }
        return false;
    }

    private static boolean notEmpty(Collection<?> values) {
        return values != null && !values.isEmpty();
    }

    private static boolean notEmpty(String value) {
        return value != null && !value.isEmpty();
    }

    public Set<String> exactIdKeys() {
        if (action == Action.REMOVE_ENCHANTMENT) return null;
        if (mod != null && !mod.isEmpty()) return null;
        if (enchantments != null && !enchantments.isEmpty()) return null;
        if (tags != null && !tags.isEmpty()) return null;
        if (pattern != null && !pattern.isEmpty()) return null;
        if (patterns != null && !patterns.isEmpty()) return null;
        Set<String> keys = new HashSet<>();
        for (Set<String> idFilters : Arrays.asList(items, blocks, fluids, effects)) {
            if (idFilters == null) continue;
            for (String filter : idFilters) {
                if (filter.startsWith("#")) return null;
                keys.add(filter);
            }
        }
        return keys.isEmpty() ? null : keys;
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
        if (tagLocation == null || itemLocation == null) return false;

        if (currentAction == Action.REMOVE_POTION) {
            return registryEntryIsInTag(BuiltInRegistries.POTION, itemLocation, cachedTag(Registries.POTION, cleanTagId, tagLocation))
                    || matchesEffectTag(tagLocation, itemLocation, registryHolder);
        } else if (currentAction == Action.REMOVE_EFFECT) {
            return matchesEffectTag(tagLocation, itemLocation, registryHolder);
        } else if (currentAction == Action.REMOVE_ENCHANTMENT) {
            return matchesEnchantmentTag(cleanTagId, tagLocation, registryHolder);
        } else if (currentAction == Action.REMOVE_PAINTING) {
            return registryHolder != null && registryHolder.value() instanceof PaintingVariant
                    && holderIsInTag(registryHolder, Registries.PAINTING_VARIANT, tagLocation);
        }
        return matchesItemLikeTag(cleanTagId, tagLocation, itemLocation, stack, registryHolder);
    }

    private boolean matchesEnchantmentTag(String cleanTagId, Identifier tagLocation, Holder<?> registryHolder) {
        if (registryHolder == null) return false;
        TagKey<Enchantment> tagKey = cachedTag(Registries.ENCHANTMENT, cleanTagId, tagLocation);
        try {
            @SuppressWarnings("unchecked")
            Holder<Enchantment> enchHolder = (Holder<Enchantment>) registryHolder;
            return enchHolder.is(tagKey);
        } catch (ClassCastException e) {
            return false;
        }
    }

    private boolean matchesItemLikeTag(String cleanTagId, Identifier tagLocation, Identifier itemLocation, ItemStack stack, Holder<?> registryHolder) {
        Set<String> cachedResolved = RuleManager.EXPANDED_TAGS_CACHE.get(cleanTagId);
        if (cachedResolved != null && cachedResolved.contains(itemLocation.toString())) return true;

        if (registryHolder != null) {
            Object value = registryHolder.value();
            if (value instanceof Block && holderIsInTag(registryHolder, Registries.BLOCK, tagLocation)) return true;
            if (value instanceof MobEffect && holderIsInTag(registryHolder, Registries.MOB_EFFECT, tagLocation)) return true;
            if (value instanceof Fluid && holderIsInTag(registryHolder, Registries.FLUID, tagLocation)) return true;
        }

        TagKey<Item> itemTag = cachedTag(Registries.ITEM, cleanTagId, tagLocation);
        if (stack != null && !stack.isEmpty() && stack.is(itemTag)) return true;
        if (registryEntryIsInTag(BuiltInRegistries.ITEM, itemLocation, itemTag)) return true;

        TagKey<Block> blockTag = TagKey.create(Registries.BLOCK, tagLocation);
        if (registryEntryIsInTag(BuiltInRegistries.BLOCK, itemLocation, blockTag)) return true;
        Item item = stack != null && !stack.isEmpty() ? stack.getItem()
                : getRegistryHolder(BuiltInRegistries.ITEM, itemLocation).map(Holder::value).orElse(null);
        if (item instanceof BlockItem blockItem && blockItem.getBlock().builtInRegistryHolder().is(blockTag)) return true;

        return registryEntryIsInTag(BuiltInRegistries.FLUID, itemLocation, TagKey.create(Registries.FLUID, tagLocation))
                || registryEntryIsInTag(BuiltInRegistries.MOB_EFFECT, itemLocation, TagKey.create(Registries.MOB_EFFECT, tagLocation));
    }

    private static boolean matchesEffectTag(Identifier tagLocation, Identifier itemLocation, Holder<?> registryHolder) {
        if (registryHolder != null && registryHolder.value() instanceof MobEffect
                && holderIsInTag(registryHolder, Registries.MOB_EFFECT, tagLocation)) {
            return true;
        }
        return registryEntryIsInTag(BuiltInRegistries.MOB_EFFECT, itemLocation, TagKey.create(Registries.MOB_EFFECT, tagLocation));
    }

    @SuppressWarnings("unchecked")
    private static <T> boolean holderIsInTag(Holder<?> holder, ResourceKey<? extends Registry<T>> registry, Identifier tagLocation) {
        return ((Holder<T>) holder).is(TagKey.create(registry, tagLocation));
    }

    private static <T> boolean registryEntryIsInTag(Registry<T> registry, Identifier id, TagKey<T> tag) {
        return getRegistryHolder(registry, id).map(holder -> holder.is(tag)).orElse(false);
    }

    @SuppressWarnings("unchecked")
    private <T> TagKey<T> cachedTag(ResourceKey<? extends Registry<T>> registry, String cleanTagId, Identifier tagLocation) {
        Map<String, TagKey<?>> cache = compiledTags;
        if (cache == null) {
            synchronized (this) {
                if (compiledTags == null) compiledTags = new ConcurrentHashMap<>();
                cache = compiledTags;
            }
        }
        return (TagKey<T>) cache.computeIfAbsent(registry + "|" + cleanTagId, k -> TagKey.create(registry, tagLocation));
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

        if (this.action == Action.REMOVE_PAINTING) return;
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