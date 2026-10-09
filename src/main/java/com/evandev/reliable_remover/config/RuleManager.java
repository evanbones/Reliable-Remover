package com.evandev.reliable_remover.config;

import com.evandev.reliable_recipes.api.ReliableRecipesAPI;
import com.evandev.reliable_recipes.config.ConfigSync;
import com.evandev.reliable_remover.Constants;
import com.evandev.reliable_remover.data.Action;
import com.evandev.reliable_remover.data.RemovalRule;
import com.evandev.reliable_remover.platform.Services;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
//? if >=1.21 {
import net.minecraft.core.component.DataComponents;
//?}
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
//? if >=1.21 {
import net.minecraft.world.item.alchemy.PotionContents;
//?} else {
/*import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
*///?}
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.enchantment.Enchantment;
//? if >=1.21 {
import net.minecraft.world.item.enchantment.ItemEnchantments;
//?}
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class RuleManager {
    private static volatile Map<Action, List<RemovalRule>> RULES_BY_ACTION = new EnumMap<>(Action.class);
    private static volatile Set<String> GLOBALLY_BANNED_ITEMS = ConcurrentHashMap.newKeySet();
    private static volatile List<String> BLACKLISTED_ITEMS = List.of();
    private static volatile Set<String> CNM_CASCADE_REMOVED = ConcurrentHashMap.newKeySet();
    private static volatile int RULES_GENERATION;
    private static volatile Runnable CNM_CASCADE_RECOMPUTE_HOOK = null;
    private static volatile Runnable SYNCED_RULES_CHANGED_HOOK = null;
    private static final AtomicBoolean CREATIVE_TABS_DIRTY = new AtomicBoolean(false);
    public static final Map<String, Set<String>> EXPANDED_TAGS_CACHE = new ConcurrentHashMap<>();
    private static final ThreadLocal<Boolean> IN_CHEST_FILL = ThreadLocal.withInitial(() -> false);
    private static final Map<String, List<RemovalRule>> DYNAMIC_RULES = new ConcurrentHashMap<>();
    public static boolean MOD_INIT_PHASE = true;
    private static final String SYNCED_BLACKLIST_PATH = "../reliable_remover.json";
    private static volatile HolderLookup.Provider REGISTRIES = null;

    public static void registerDynamicRules(String sourceId, List<RemovalRule> rules) {
        if (rules == null || rules.isEmpty()) {
            DYNAMIC_RULES.remove(sourceId);
        } else {
            DYNAMIC_RULES.put(sourceId, new ArrayList<>(rules));
        }
        load();
    }

    public static void unregisterDynamicRules(String sourceId) {
        if (DYNAMIC_RULES.remove(sourceId) != null) {
            load();
        }
    }

    public static boolean isInChestFill() {
        return IN_CHEST_FILL.get();
    }

    public static void setInChestFill(boolean value) {
        IN_CHEST_FILL.set(value);
    }

    public static void load() {
        Map<Action, List<RemovalRule>> newRules = new EnumMap<>(Action.class);
        Set<String> newBanned = ConcurrentHashMap.newKeySet();
        for (Action action : Action.values()) newRules.put(action, new ArrayList<>());

        List<ConfigSync.SyncedFile> remoteFiles = ConfigSync.getRemoteFiles(Constants.MOD_ID);
        List<String> blacklist;
        if (remoteFiles != null) {
            blacklist = new ArrayList<>();
            for (ConfigSync.SyncedFile file : remoteFiles) {
                if (file.path().equals(SYNCED_BLACKLIST_PATH)) {
                    blacklist.addAll(parseSyncedBlacklist(file.content()));
                } else {
                    RuleParser.parseString(file.path(), file.content(), newRules);
                }
            }
        } else {
            Path configDir = getConfigDir();

            if (!Files.exists(configDir)) {
                try {
                    Files.createDirectories(configDir);
                } catch (Exception ignored) {
                }
            }

            boolean hasFiles = false;
            try (Stream<Path> paths = Files.walk(configDir)) {
                List<Path> files = paths.filter(Files::isRegularFile).filter(p -> p.toString().endsWith(".json")).toList();
                if (!files.isEmpty()) {
                    hasFiles = true;
                    files.forEach(path -> RuleParser.parseFile(path, newRules));
                }
            } catch (Exception e) {
                Constants.LOG.error("Failed to load removal rules", e);
            }

            if (!hasFiles) generateDefaultConfig(configDir);
            blacklist = ModConfig.get().blacklistedItems;
        }

        if (!MOD_INIT_PHASE) {
            validateRules(newRules);
            if (blacklist != null) {
                List<String> validBlacklist = new ArrayList<>(blacklist);
                validBlacklist.removeIf(itemId -> {
                    if (itemId.startsWith("#")) return false;
                    Identifier id = Identifier.tryParse(itemId);
                    if (id == null) {
                        Constants.LOG.warn("Reliable Remover: Skipping invalid blacklisted item ID '{}'.", itemId);
                        return true;
                    }
                    if (!BuiltInRegistries.ITEM.containsKey(id)
                            && !BuiltInRegistries.BLOCK.containsKey(id)
                            && !BuiltInRegistries.FLUID.containsKey(id)
                            && !BuiltInRegistries.MOB_EFFECT.containsKey(id)) {
                        Constants.LOG.warn("Reliable Remover: Skipping invalid blacklisted item/block/fluid/effect ID '{}'.", itemId);
                        return true;
                    }
                    return false;
                });
                if (remoteFiles == null) {
                    ModConfig.get().blacklistedItems = validBlacklist;
                }
                blacklist = validBlacklist;
            }
        }

        for (List<RemovalRule> dynList : DYNAMIC_RULES.values()) {
            for (RemovalRule rule : dynList) {
                if (rule.actions != null && !rule.actions.isEmpty()) {
                    for (Action act : rule.actions) {
                        newRules.computeIfAbsent(act, a -> new ArrayList<>()).add(rule);
                    }
                } else {
                    Action act = rule.action != null ? rule.action : Action.REMOVE;
                    newRules.computeIfAbsent(act, a -> new ArrayList<>()).add(rule);
                }
            }
        }

        optimizeRules(newRules, newBanned);

        RULES_BY_ACTION = newRules;
        GLOBALLY_BANNED_ITEMS = newBanned;
        if (blacklist != null) {
            GLOBALLY_BANNED_ITEMS.addAll(blacklist);
        }
        BLACKLISTED_ITEMS = blacklist != null ? List.copyOf(blacklist) : List.of();
        CNM_CASCADE_REMOVED = ConcurrentHashMap.newKeySet();

        int ruleCount = RULES_BY_ACTION.values().stream().mapToInt(List::size).sum() + GLOBALLY_BANNED_ITEMS.size();
        Constants.LOG.info("Loaded {} reliable remover rules.", ruleCount);

        ReliableRecipesAPI.clearItemReplacements();
        for (List<RemovalRule> rules : RULES_BY_ACTION.values()) {
            for (RemovalRule rule : rules) {
                if (rule.hasReplacement() && rule.items != null) {
                    for (String item : rule.items) {
                        ReliableRecipesAPI.registerItemReplacement(item, rule.getReplacementItemId());
                    }
                }
            }
        }

        if (CNM_CASCADE_RECOMPUTE_HOOK != null) CNM_CASCADE_RECOMPUTE_HOOK.run();
        RULES_GENERATION++;

        if (Services.PLATFORM.isPhysicalClient()) {
            CREATIVE_TABS_DIRTY.set(true);
        }
    }

    private static Path getConfigDir() {
        return Services.PLATFORM.getConfigDirectory().resolve("reliable_remover");
    }

    /**
     * Reads the rule files and blacklist to send to clients. See {@link ConfigSync}.
     */
    public static List<ConfigSync.SyncedFile> createSyncSnapshot() {
        List<ConfigSync.SyncedFile> files = new ArrayList<>(ConfigSync.readDirectory(getConfigDir()));

        JsonArray items = new JsonArray();
        List<String> blacklist = ModConfig.get().blacklistedItems;
        if (blacklist != null) {
            for (String item : List.copyOf(blacklist)) items.add(item);
        }
        JsonObject root = new JsonObject();
        root.add("blacklistedItems", items);
        files.add(new ConfigSync.SyncedFile(SYNCED_BLACKLIST_PATH, root.toString()));
        return files;
    }

    public static void onSyncedRulesChanged() {
        if (REGISTRIES != null) {
            expandTagRules(REGISTRIES);
        } else {
            load();
        }

        if (SYNCED_RULES_CHANGED_HOOK != null) SYNCED_RULES_CHANGED_HOOK.run();
    }

    public static void registerSyncedRulesChanged(Runnable hook) {
        SYNCED_RULES_CHANGED_HOOK = hook;
    }

    private static List<String> parseSyncedBlacklist(String content) {
        List<String> items = new ArrayList<>();
        try {
            JsonObject root = JsonParser.parseString(content).getAsJsonObject();
            if (root.has("blacklistedItems")) {
                for (JsonElement item : root.getAsJsonArray("blacklistedItems")) {
                    items.add(item.getAsString());
                }
            }
        } catch (Exception e) {
            Constants.LOG.error("Failed to read blacklist from server", e);
        }
        return items;
    }

    public static boolean consumeCreativeTabsDirty() {
        return CREATIVE_TABS_DIRTY.getAndSet(false);
    }

    private static void generateDefaultConfig(Path configDir) {
        String defaultJson = """
                [
                    {
                        "action": "remove",
                        "items": [
                            "examplemod:item1",
                            "examplemod:item2"
                        ]
                    }
                ]""";
        try {
            Files.writeString(configDir.resolve("removal_example.json.disabled"), defaultJson);
            Constants.LOG.info("Created example config at config/reliable_remover/removal_example.json.disabled");
        } catch (Exception e) {
            Constants.LOG.error("Failed to generate default rule", e);
        }
    }

    private static void validateRules(Map<Action, List<RemovalRule>> rulesByAction) {
        for (List<RemovalRule> rules : rulesByAction.values()) {
            for (RemovalRule rule : rules) {
                if (rule.items != null) {
                    rule.items.removeIf(itemId -> {
                        if (itemId.startsWith("#")) return false;

                        Identifier id = Identifier.tryParse(itemId);
                        if (id == null) {
                            Constants.LOG.warn("Reliable Remover: Skipping completely invalid ID '{}'.", itemId);
                            return true;
                        }

                        if (rule.action == Action.REMOVE_POTION || rule.action == Action.REMOVE_EFFECT) {
                            if (!BuiltInRegistries.POTION.containsKey(id) && !BuiltInRegistries.MOB_EFFECT.containsKey(id)) {
                                Constants.LOG.warn("Reliable Remover: Skipping invalid potion/effect ID '{}'.", itemId);
                                return true;
                            }
                        } else if (rule.action == Action.REMOVE_ENCHANTMENT) {
                            return false;
                        } else {
                            if (!BuiltInRegistries.ITEM.containsKey(id)
                                    && !BuiltInRegistries.BLOCK.containsKey(id)
                                    && !BuiltInRegistries.FLUID.containsKey(id)
                                    && !BuiltInRegistries.MOB_EFFECT.containsKey(id)) {
                                Constants.LOG.warn("Reliable Remover: Skipping invalid item/block/fluid/effect ID '{}'.", itemId);
                                return true;
                            }
                        }
                        return false;
                    });
                }

                if (rule.enchantments != null) {
                    rule.enchantments.removeIf(enchId -> {
                        if (enchId.startsWith("#")) return false;
                        Identifier id = Identifier.tryParse(enchId);
                        if (id == null) {
                            Constants.LOG.warn("Reliable Remover: Skipping invalid enchantment ID '{}'.", enchId);
                            return true;
                        }
                        return false;
                    });
                }

                if (rule.tags != null) {
                    rule.tags.removeIf(tagId -> {
                        String cleanTagId = tagId.startsWith("#") ? tagId.substring(1) : tagId;
                        Identifier id = Identifier.tryParse(cleanTagId);
                        if (id == null) {
                            Constants.LOG.warn("Reliable Remover: Skipping invalid tag ID '{}'.", tagId);
                            return true;
                        }
                        return false;
                    });
                }
            }
        }
    }

    private static void optimizeRules(Map<Action, List<RemovalRule>> rulesByAction, Set<String> globallyBannedItems) {
        List<RemovalRule> removeRules = rulesByAction.get(Action.REMOVE);
        if (removeRules == null) return;
        Iterator<RemovalRule> iterator = removeRules.iterator();
        while (iterator.hasNext()) {
            RemovalRule rule = iterator.next();
            if (isSimpleRule(rule)) {
                globallyBannedItems.addAll(rule.items);
                iterator.remove();
            }
        }
    }

    private static boolean isSimpleRule(RemovalRule rule) {
        return (rule.dimensions == null || rule.dimensions.isEmpty()) &&
                (rule.entities == null || rule.entities.isEmpty()) &&
                (rule.mod == null || rule.mod.isEmpty()) &&
                (rule.pattern == null || rule.pattern.isEmpty()) &&
                (rule.patterns == null || rule.patterns.isEmpty()) &&
                (rule.tags == null || rule.tags.isEmpty()) &&
                (rule.nbt == null || rule.nbt.isEmpty()) &&
                (rule.registry == null || rule.registry.isEmpty()) &&
                (rule.tagType == null || rule.tagType.isEmpty()) &&
                rule.not == null &&
                !rule.hasReplacement() &&
                (rule.enchantments == null || rule.enchantments.isEmpty()) &&
                (rule.effects == null || rule.effects.isEmpty()) &&
                (rule.blocks == null || rule.blocks.isEmpty()) &&
                (rule.fluids == null || rule.fluids.isEmpty()) &&
                rule.items != null && !rule.items.isEmpty() &&
                rule.items.stream().noneMatch(id -> id.startsWith("#"));
    }

    public static Map<Action, List<RemovalRule>> getRulesByAction() {
        return RULES_BY_ACTION;
    }

    /**
     * The blacklisted items in effect. Use this rather than the config's list, which is only the local one and is
     * ignored while connected to a server.
     */
    public static List<String> getBlacklistedItems() {
        return BLACKLISTED_ITEMS;
    }

    public static boolean isHidden(ItemStack stack) {
        return isHidden(stack, null, null, "item");
    }

    public static boolean isHidden(ItemStack stack, String context) {
        return isHidden(stack, null, null, context);
    }

    public static boolean isHidden(ItemStack stack, Level level) {
        return isHidden(stack, level, null, "item");
    }

    public static boolean isHidden(ItemStack stack, Level level, Entity holder) {
        return isHidden(stack, level, holder, "item");
    }

    public static boolean isHidden(ItemStack stack, Level level, Entity holder, String context) {
        if (stack == null || stack.isEmpty()) return false;

        Identifier itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        String id = itemId.toString();

        if (GLOBALLY_BANNED_ITEMS.contains(id)) return true;
        if (CNM_CASCADE_REMOVED.contains(id)) return true;

        if (BuiltInRegistries.ITEM.containsKey(itemId)) {
            String dim = level != null ? level.dimension().identifier().toString() : null;
            if (checkRules(stack, id, Action.REMOVE, dim, holder, null, context)) return true;
        }

        //? if >=1.21 {
        if (id.equals("minecraft:enchanted_book")) {
            if (stack.has(DataComponents.STORED_ENCHANTMENTS)) {
                ItemEnchantments enchantments = stack.get(DataComponents.STORED_ENCHANTMENTS);
                if (enchantments != null && !enchantments.isEmpty()) {
                    boolean allBlocked = true;
                    for (var entry : enchantments.entrySet()) {
                        if (!isEnchantmentBlocked(stack, entry.getKey())) {
                            allBlocked = false;
                            break;
                        }
                    }
                    if (allBlocked) return true;
                }
            }
        }

        if (stack.has(DataComponents.POTION_CONTENTS)) {
            PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
            if (contents != null) {
                String dim = level != null ? level.dimension().identifier().toString() : null;
                String potionId = contents.potion().flatMap(Holder::unwrapKey).map(key -> key.identifier().toString()).orElse(null);
                if (potionId != null && checkRules(null, potionId, Action.REMOVE_POTION, dim, holder, null, context))
                    return true;
                for (MobEffectInstance effectInst : contents.getAllEffects()) {
                    Holder<MobEffect> effectHolder = effectInst.getEffect();
                    Identifier loc = BuiltInRegistries.MOB_EFFECT.getKey(effectHolder.value());
                    if (loc != null) {
                        String effectId = loc.toString();
                        if (checkRules(null, effectId, Action.REMOVE_POTION, dim, holder, effectHolder, context))
                            return true;
                    }
                    if (isEffectBlocked(effectHolder, level, holder)) return true;
                }
            }
        }
        //?} else {
        /*CompoundTag tag = stack.getTag();
        if (tag == null) return false;

        if (id.equals("minecraft:enchanted_book")) {
            ListTag enchantments = tag.getList("StoredEnchantments", Tag.TAG_COMPOUND);
            if (!enchantments.isEmpty()) {
                boolean allBlocked = true;
                for (int i = 0; i < enchantments.size(); i++) {
                    if (!isEnchantmentBlocked(stack, getEnchantmentHolder(enchantments.getCompound(i)))) {
                        allBlocked = false;
                        break;
                    }
                }
                if (allBlocked) return true;
            }
        }

        if (tag.contains("Potion") || tag.contains("CustomPotionEffects")) {
            String dim = level != null ? level.dimension().identifier().toString() : null;
            Potion potion = PotionUtils.getPotion(stack);
            String potionId = potion == Potions.EMPTY ? null : BuiltInRegistries.POTION.getKey(potion).toString();
            if (potionId != null && checkRules(null, potionId, Action.REMOVE_POTION, dim, holder, null, context))
                return true;
            for (MobEffectInstance effectInst : PotionUtils.getMobEffects(stack)) {
                Holder<MobEffect> effectHolder = BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effectInst.getEffect());
                Identifier loc = BuiltInRegistries.MOB_EFFECT.getKey(effectHolder.value());
                if (loc != null) {
                    String effectId = loc.toString();
                    if (checkRules(null, effectId, Action.REMOVE_POTION, dim, holder, effectHolder, context))
                        return true;
                }
                if (isEffectBlocked(effectHolder, level, holder)) return true;
            }
        }
        *///?}
        return false;
    }

    public static void stripBlockedEnchantments(ItemStack stack) {
        stripBlockedEnchantments(stack, null, RandomSource.create());
    }

    public static void stripBlockedEnchantments(ItemStack stack, Level level) {
        if (level != null) {
            stripBlockedEnchantments(stack, level.registryAccess(), level.getRandom());
        } else {
            stripBlockedEnchantments(stack);
        }
    }

    public static void stripBlockedEnchantments(ItemStack stack, RegistryAccess registryAccess, RandomSource random) {
        if (stack == null || stack.isEmpty()) return;

        //? if >=1.21 {
        if (stack.has(DataComponents.STORED_ENCHANTMENTS)) {
            ItemEnchantments enchantments = stack.get(DataComponents.STORED_ENCHANTMENTS);
            if (enchantments != null) {
                boolean changed = false;
                ItemEnchantments.Mutable validEnchantments = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);

                for (var entry : enchantments.entrySet()) {
                    if (isEnchantmentBlocked(stack, entry.getKey())) {
                        changed = true;
                    } else {
                        validEnchantments.set(entry.getKey(), entry.getIntValue());
                    }
                }

                if (changed) {
                    ItemEnchantments result = validEnchantments.toImmutable();
                    if (result.isEmpty() && registryAccess != null) {
                        Holder<Enchantment> rerolled = getRandomAllowedEnchantment(registryAccess, random, null);
                        if (rerolled != null) {
                            int level = Mth.nextInt(random, rerolled.value().getMinLevel(), rerolled.value().getMaxLevel());
                            validEnchantments.set(rerolled, level);
                            result = validEnchantments.toImmutable();
                        }
                    }
                    if (result.isEmpty()) {
                        stack.remove(DataComponents.STORED_ENCHANTMENTS);
                    } else {
                        stack.set(DataComponents.STORED_ENCHANTMENTS, result);
                    }
                }
            }
        }

        if (stack.has(DataComponents.ENCHANTMENTS)) {
            ItemEnchantments enchantments = stack.get(DataComponents.ENCHANTMENTS);
            if (enchantments != null) {
                boolean changed = false;
                ItemEnchantments.Mutable validEnchantments = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);

                for (var entry : enchantments.entrySet()) {
                    if (isEnchantmentBlocked(stack, entry.getKey())) {
                        changed = true;
                    } else {
                        validEnchantments.set(entry.getKey(), entry.getIntValue());
                    }
                }

                if (changed) {
                    ItemEnchantments result = validEnchantments.toImmutable();
                    if (result.isEmpty()) {
                        stack.remove(DataComponents.ENCHANTMENTS);
                    } else {
                        stack.set(DataComponents.ENCHANTMENTS, result);
                    }
                }
            }
        }
        //?} else {
        /*CompoundTag tag = stack.getTag();
        if (tag == null) return;

        if (tag.contains("StoredEnchantments", Tag.TAG_LIST)) {
            ListTag enchantments = tag.getList("StoredEnchantments", Tag.TAG_COMPOUND);
            ListTag validEnchantments = filterBlockedEnchantments(stack, enchantments);

            if (validEnchantments.size() != enchantments.size()) {
                if (validEnchantments.isEmpty() && registryAccess != null) {
                    Holder<Enchantment> rerolled = getRandomAllowedEnchantment(registryAccess, random, null);
                    if (rerolled != null) {
                        int level = Mth.nextInt(random, rerolled.value().getMinLevel(), rerolled.value().getMaxLevel());
                        validEnchantments.add(EnchantmentHelper.storeEnchantment(EnchantmentHelper.getEnchantmentId(rerolled.value()), level));
                    }
                }
                if (validEnchantments.isEmpty()) {
                    stack.removeTagKey("StoredEnchantments");
                } else {
                    tag.put("StoredEnchantments", validEnchantments);
                }
            }
        }

        if (tag.contains("Enchantments", Tag.TAG_LIST)) {
            ListTag enchantments = tag.getList("Enchantments", Tag.TAG_COMPOUND);
            ListTag validEnchantments = filterBlockedEnchantments(stack, enchantments);

            if (validEnchantments.size() != enchantments.size()) {
                if (validEnchantments.isEmpty()) {
                    stack.removeTagKey("Enchantments");
                } else {
                    tag.put("Enchantments", validEnchantments);
                }
            }
        }
        *///?}
    }

    public static Holder<Enchantment> getRandomAllowedEnchantment(RegistryAccess registryAccess, RandomSource random, Predicate<Holder<Enchantment>> extraFilter) {
        if (registryAccess == null) return null;
        //? if <1.19.3 {
        /*var lookupOpt = HolderLookup.Provider.of(registryAccess).lookup(Registries.ENCHANTMENT);
        *///?} else {
        var lookupOpt = registryAccess.lookup(Registries.ENCHANTMENT);
        //?}
        if (lookupOpt.isEmpty()) return null;
        var lookup = lookupOpt.get();
        List<Holder<Enchantment>> candidates = lookup.listElements()
                .map(h -> (Holder<Enchantment>) h)
                .filter(h -> !isEnchantmentBlocked(h))
                .filter(h -> extraFilter == null || extraFilter.test(h))
                .toList();
        if (candidates.isEmpty() && extraFilter != null) {
            candidates = lookup.listElements()
                    .map(h -> (Holder<Enchantment>) h)
                    .filter(h -> !isEnchantmentBlocked(h))
                    .toList();
        }
        if (candidates.isEmpty()) return null;
        return candidates.get(random.nextInt(candidates.size()));
    }

    //? if <1.21 {
    /*private static ListTag filterBlockedEnchantments(ItemStack stack, ListTag enchantments) {
        ListTag valid = new ListTag();
        for (int i = 0; i < enchantments.size(); i++) {
            CompoundTag entry = enchantments.getCompound(i);
            if (!isEnchantmentBlocked(stack, getEnchantmentHolder(entry))) {
                valid.add(entry);
            }
        }
        return valid;
    }

    public static Holder<Enchantment> getEnchantmentHolder(CompoundTag enchantmentTag) {
        Identifier id = EnchantmentHelper.getEnchantmentId(enchantmentTag);
        if (id == null) return null;
        return BuiltInRegistries.ENCHANTMENT.getHolder(net.minecraft.resources.ResourceKey.create(Registries.ENCHANTMENT, id)).orElse(null);
    }

    public static boolean isEnchantmentBlocked(ItemStack stack, Enchantment enchantment) {
        return isEnchantmentBlocked(stack, BuiltInRegistries.ENCHANTMENT.wrapAsHolder(enchantment));
    }

    public static boolean isEffectBlocked(MobEffect effect, Level level, Entity entity) {
        return isEffectBlocked(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect), level, entity);
    }

    public static boolean isEffectCreativeBlocked(MobEffect effect, Entity entity) {
        return isEffectCreativeBlocked(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect), entity);
    }

    public static Holder<MobEffect> getEffectReplacement(MobEffect effect, Level level, Entity entity) {
        return getEffectReplacement(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect), level, entity);
    }
    *///?}

    public static boolean isAttackBlocked(ItemStack stack, Level level, Entity target) {
        if (stack == null || stack.isEmpty()) return false;
        String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        String dim = level != null ? level.dimension().identifier().toString() : null;
        return checkRules(stack, id, Action.REMOVE_ATTACKS, dim, target, null, "attack");
    }

    public static boolean isInteractionBlocked(ItemStack stack, Level level, Entity target) {
        if (stack == null || stack.isEmpty()) return false;
        String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        String dim = level != null ? level.dimension().identifier().toString() : null;
        return checkRules(stack, id, Action.REMOVE_INTERACTIONS, dim, target, null, "interaction");
    }

    public static boolean isBlockInteractionBlocked(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (state == null || state.isAir()) return false;
        Identifier loc = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        String id = loc.toString();
        String dim = level != null ? level.dimension().identifier().toString() : null;
        return checkRules(null, id, Action.REMOVE_INTERACTIONS, dim, entity, state.getBlock().builtInRegistryHolder(), "block_interaction");
    }

    public static boolean isBlockInteractionBlocked(BlockState state, Level level) {
        return isBlockInteractionBlocked(state, level, null, null);
    }

    public static boolean isPlacementBlocked(ItemStack stack, Level level, Entity entity) {
        if (stack == null || stack.isEmpty()) return false;
        String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        String dim = level != null ? level.dimension().identifier().toString() : null;
        return checkRules(stack, id, Action.REMOVE_PLACEMENT, dim, entity, null, "placement");
    }

    public static boolean isPlacementBlocked(ItemStack stack, Level level) {
        return isPlacementBlocked(stack, level, null);
    }

    public static boolean isTradeBlocked(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !ModConfig.get().removeItemsFromTrades) return false;
        if (isHidden(stack)) return true;
        if (getReplacement(stack, Action.REMOVE_TRADE, null, null, "trade") != null) return false;
        return checkRules(stack, BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(), Action.REMOVE_TRADE, null, null, null, "trade");
    }

    public static boolean isLootBlocked(ItemStack stack) {
        return isLootBlocked(stack, null);
    }

    public static boolean isLootBlocked(ItemStack stack, LootParams context) {
        if (stack == null || stack.isEmpty() || !ModConfig.get().removeItemsFromLootChests) return false;
        String dim = context != null ? context.getLevel().dimension().identifier().toString() : null;
        Entity entity = getLootEntity(context);
        Level level = context != null ? context.getLevel() : null;
        if (isHidden(stack, level, entity, "loot")) return true;
        if (getLootReplacement(stack, context) != null) return false;
        String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();

        if (isInChestFill()) {
            if (checkRules(stack, id, Action.REMOVE_CHEST_LOOT, dim, entity, null, "chest_loot")) return true;
        }
        return checkRules(stack, id, Action.REMOVE_LOOT, dim, entity, null, "loot");
    }

    private static Entity getLootEntity(LootParams context) {
        if (context == null) return null;
        //? if >=26.3 {
        /*return context.contextMap().get(LootContextParams.THIS_ENTITY);
         *///?} else if >=26.1 {
        return context.contextMap().getOptional(LootContextParams.THIS_ENTITY);
        //?} else {
        /*return context.getParamOrNull(LootContextParams.THIS_ENTITY);
         *///?}
    }

    public static boolean isInventoryBlocked(ItemStack stack) {
        return isInventoryBlocked(stack, null, null);
    }

    public static boolean isInventoryBlocked(ItemStack stack, Level level, Entity holder) {
        if (stack == null || stack.isEmpty() || !ModConfig.get().removeItemsFromInventories) return false;
        if (isHidden(stack, level, holder)) return true;
        if (getReplacement(stack, Action.REMOVE_INVENTORY, level, holder, "inventory") != null) return false;
        return checkRules(stack, BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(), Action.REMOVE_INVENTORY, level != null ? level.dimension().identifier().toString() : null, holder, null, "inventory");
    }

    public static boolean isCreativeBlocked(ItemStack stack) {
        return isCreativeBlocked(stack, null);
    }

    public static boolean isCreativeBlocked(ItemStack stack, Entity player) {
        if (stack == null || stack.isEmpty() || !ModConfig.get().removeItemsFromCreativeTabs) return false;
        if (isHidden(stack, null, player)) return true;
        if (getReplacement(stack, Action.REMOVE_CREATIVE, null, player, "creative") != null) return false;
        return checkRules(stack, BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(), Action.REMOVE_CREATIVE, null, player, null, "creative");
    }

    public static boolean isDropsBlocked(ItemStack stack, Level level, Entity entity) {
        if (stack == null || stack.isEmpty() || !ModConfig.get().removeDroppedItems) return false;
        if (isHidden(stack, level, entity)) return true;
        if (getReplacement(stack, Action.REMOVE_DROPS, level, entity, "drops") != null) return false;
        return checkRules(stack, BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(), Action.REMOVE_DROPS, level != null ? level.dimension().identifier().toString() : null, entity, null, "drops");
    }

    public static boolean isEquipmentBlocked(ItemStack stack, Level level, Entity entity) {
        if (stack == null || stack.isEmpty() || !ModConfig.get().removeMobEquipment) return false;
        if (isHidden(stack, level, entity)) return true;
        if (getReplacement(stack, Action.REMOVE_EQUIPMENT, level, entity, "equipment") != null) return false;
        return checkRules(stack, BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(), Action.REMOVE_EQUIPMENT, level != null ? level.dimension().identifier().toString() : null, entity, null, "equipment");
    }

    public static boolean isStorageBlocked(ItemStack stack, Level level) {
        if (stack == null || stack.isEmpty() || !ModConfig.get().removeItemsFromStorage) return false;
        if (getReplacement(stack, Action.REMOVE_STORAGE, level, null, "storage") != null) return false;
        if (isHidden(stack, level)) return true;
        String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        String dim = level != null ? level.dimension().identifier().toString() : null;
        return checkRules(stack, id, Action.REMOVE_STORAGE, dim, null, null, "storage");
    }

    public static boolean isHandSwingBlocked(ItemStack stack, Level level) {
        if (stack == null || stack.isEmpty()) return false;
        String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        String dim = level != null ? level.dimension().identifier().toString() : null;
        return checkRules(stack, id, Action.REMOVE_HAND_SWING, dim, null, null, "swing");
    }

    public static boolean isInfoBlocked(ItemStack stack) {
        return isInfoBlocked(stack, null);
    }

    public static boolean isInfoBlocked(ItemStack stack, Entity player) {
        if (stack == null || stack.isEmpty()) return false;
        String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        return checkRules(stack, id, Action.REMOVE_INFO, null, player, null, "info");
    }

    public static boolean isRecipeBlocked(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !ModConfig.get().removeRecipes) return false;
        if (getReplacement(stack, Action.REMOVE_RECIPE, null, null, "recipe") != null) return false;
        if (isHidden(stack)) return true;
        String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        return checkRules(stack, id, Action.REMOVE_RECIPE, null, null, null, "recipe");
    }

    //? if >=26.1 {
    public static boolean isRecipeBlocked(Recipe<?> recipe) {
        if (!ModConfig.get().removeRecipes) return false;
        List<ItemStack> outputs = ReliableRecipesAPI.getRecipeResults(recipe);
        if (outputs.isEmpty()) return false;
        for (ItemStack stack : outputs) {
            if (!stack.isEmpty() && !isRecipeBlocked(stack)) return false;
        }
        return true;
    }
    //?}

    public static boolean isEnchantmentBlocked(Holder<Enchantment> enchantment) {
        return isEnchantmentBlocked(null, enchantment);
    }

    public static boolean isEnchantmentBlocked(ItemStack stack, Holder<Enchantment> enchantment) {
        if (enchantment == null) return false;
        String id = enchantment.unwrapKey().map(key -> key.identifier().toString()).orElse("");
        return checkRules(stack, id, Action.REMOVE_ENCHANTMENT, null, null, enchantment, "enchantment");
    }

    public static ItemStack getReplacement(ItemStack stack, Action action, Level level, Entity holder, String context) {
        if (stack == null || stack.isEmpty() || !isRemovalEnabled(action)) return null;
        String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        String dim = level != null ? level.dimension().identifier().toString() : null;
        RemovalRule rule = getMatchingRule(stack, id, action, dim, holder, null, context);
        if (rule == null && action != Action.REMOVE)
            rule = getMatchingRule(stack, id, Action.REMOVE, dim, holder, null, context);

        return rule != null ? rule.createReplacement(stack) : null;
    }

    private static boolean isRemovalEnabled(Action action) {
        ModConfig config = ModConfig.get();
        return switch (action) {
            case REMOVE_INVENTORY -> config.removeItemsFromInventories;
            case REMOVE_DROPS -> config.removeDroppedItems;
            case REMOVE_STORAGE -> config.removeItemsFromStorage;
            case REMOVE_TRADE -> config.removeItemsFromTrades;
            case REMOVE_CREATIVE -> config.removeItemsFromCreativeTabs;
            case REMOVE_RECIPE -> config.removeRecipes;
            case REMOVE_EQUIPMENT -> config.removeMobEquipment;
            default -> true;
        };
    }

    public static ItemStack getLootReplacement(ItemStack stack, LootParams context) {
        if (stack == null || stack.isEmpty() || !ModConfig.get().removeItemsFromLootChests) return null;
        String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        String dim = context != null ? context.getLevel().dimension().identifier().toString() : null;
        Entity entity = getLootEntity(context);
        RemovalRule rule = null;
        if (isInChestFill())
            rule = getMatchingRule(stack, id, Action.REMOVE_CHEST_LOOT, dim, entity, null, "chest_loot");
        if (rule == null) rule = getMatchingRule(stack, id, Action.REMOVE_LOOT, dim, entity, null, "loot");
        if (rule == null) rule = getMatchingRule(stack, id, Action.REMOVE, dim, entity, null, "item");

        return rule != null ? rule.createReplacement(stack) : null;
    }

    private static boolean checkRules(ItemStack stack, String itemId, Action action, String dimension, Entity target, Holder<?> registryHolder, String context) {
        return getMatchingRule(stack, itemId, action, dimension, target, registryHolder, context) != null;
    }

    private static RemovalRule getMatchingRule(ItemStack stack, String itemId, Action action, String dimension, Entity target, Holder<?> registryHolder, String context) {
        String entityId = target != null ? BuiltInRegistries.ENTITY_TYPE.getKey(target.getType()).toString() : null;
        List<RemovalRule> rules = RULES_BY_ACTION.get(action);
        if (rules == null || rules.isEmpty()) return null;
        for (RemovalRule rule : rules) {
            if (rule.action == action && rule.matches(stack, itemId, dimension, entityId, target, registryHolder, context))
                return rule;
        }
        return null;
    }

    public static HolderLookup.Provider getRegistries() {
        return REGISTRIES;
    }

    //? if <1.19.3 {
    /*public static void expandTagRules(net.minecraft.core.RegistryAccess registryAccess) {
        expandTagRules(HolderLookup.Provider.of(registryAccess));
    }

    *///?}
    public static void expandTagRules(HolderLookup.Provider registries) {
        REGISTRIES = registries;
        RuleManager.load();
        for (List<RemovalRule> rules : RULES_BY_ACTION.values()) {
            for (RemovalRule rule : rules) {
                rule.expandTags(registries);
            }
        }
        int totalExpandedItems = RULES_BY_ACTION.values().stream()
                .flatMap(List::stream)
                .mapToInt(rule -> rule.items != null ? rule.items.size() : 0)
                .sum();
        Constants.LOG.info("Total items removed: {}", totalExpandedItems + GLOBALLY_BANNED_ITEMS.size());

        if (CNM_CASCADE_RECOMPUTE_HOOK != null) CNM_CASCADE_RECOMPUTE_HOOK.run();
        RULES_GENERATION++;
    }

    public static void registerCnmCascadeRecompute(Runnable hook) {
        CNM_CASCADE_RECOMPUTE_HOOK = hook;
    }

    public static void setCnmCascadeRemoved(Set<String> items) {
        CNM_CASCADE_REMOVED = items;
        RULES_GENERATION++;
    }

    public static int getRulesGeneration() {
        return RULES_GENERATION;
    }

    public static boolean isFluidHidden(String fluidId) {
        if (fluidId == null || fluidId.isEmpty()) return false;
        if (GLOBALLY_BANNED_ITEMS.contains(fluidId)) return true;
        if (CNM_CASCADE_REMOVED.contains(fluidId)) return true;
        return checkRules(null, fluidId, Action.REMOVE, null, null, null, "item");
    }

    public static boolean isEffectBlocked(Holder<MobEffect> effectHolder, Level level, Entity entity) {
        if (effectHolder == null) return false;
        MobEffect effect = effectHolder.value();
        Identifier loc = BuiltInRegistries.MOB_EFFECT.getKey(effect);
        if (loc == null) return false;
        String id = loc.toString();
        if (GLOBALLY_BANNED_ITEMS.contains(id)) return true;
        if (CNM_CASCADE_REMOVED.contains(id)) return true;
        if (getEffectReplacement(effectHolder, level, entity) != null) return false;
        String dim = level != null ? level.dimension().identifier().toString() : null;
        if (checkRules(null, id, Action.REMOVE_EFFECT, dim, entity, effectHolder, "effect")) return true;
        return checkRules(null, id, Action.REMOVE, dim, entity, effectHolder, "effect");
    }

    /**
     * Resolves the effect a removed effect should be swapped for via {@code replace_with}, following
     * chains (A -> B -> C). Returns null when there is no replacement or the chain loops back on itself.
     */
    public static Holder<MobEffect> getEffectReplacement(Holder<MobEffect> effectHolder, Level level, Entity entity) {
        if (effectHolder == null) return null;
        String dim = level != null ? level.dimension().identifier().toString() : null;
        Set<String> seen = new HashSet<>();
        Holder<MobEffect> current = effectHolder;
        Holder<MobEffect> replacement = null;
        while (true) {
            Identifier loc = BuiltInRegistries.MOB_EFFECT.getKey(current.value());
            if (loc == null) return replacement;
            String id = loc.toString();
            if (!seen.add(id)) return null;

            RemovalRule rule = getMatchingRule(null, id, Action.REMOVE_EFFECT, dim, entity, current, "effect");
            if (rule == null) rule = getMatchingRule(null, id, Action.REMOVE, dim, entity, current, "effect");
            if (rule == null || !rule.hasReplacement()) return replacement;

            Identifier replacementId = Identifier.tryParse(rule.replaceWith.trim());
            if (replacementId == null) return replacement;
            Holder<MobEffect> next = RemovalRule.getRegistryHolder(BuiltInRegistries.MOB_EFFECT, replacementId).orElse(null);
            if (next == null) return replacement;
            replacement = next;
            current = next;
        }
    }

    public static boolean isEffectBlocked(Holder<MobEffect> effectHolder, Level level) {
        return isEffectBlocked(effectHolder, level, null);
    }

    public static boolean isEffectCreativeBlocked(Holder<MobEffect> effectHolder, Entity entity) {
        if (effectHolder == null) return false;
        MobEffect effect = effectHolder.value();
        Identifier loc = BuiltInRegistries.MOB_EFFECT.getKey(effect);
        if (loc == null) return false;
        String id = loc.toString();
        if (GLOBALLY_BANNED_ITEMS.contains(id)) return true;
        if (CNM_CASCADE_REMOVED.contains(id)) return true;
        if (checkRules(null, id, Action.REMOVE_CREATIVE, null, entity, effectHolder, "creative")) return true;
        if (checkRules(null, id, Action.REMOVE_EFFECT, null, entity, effectHolder, "effect")) return true;
        return checkRules(null, id, Action.REMOVE, null, entity, effectHolder, "effect");
    }

    public static boolean isEffectCreativeBlocked(Holder<MobEffect> effectHolder) {
        return isEffectCreativeBlocked(effectHolder, null);
    }
}