package com.evandev.reliable_remover.config;

import com.evandev.reliable_recipes.api.ReliableRecipesAPI;
import com.evandev.reliable_remover.Constants;
import com.evandev.reliable_remover.data.Action;
import com.evandev.reliable_remover.data.RemovalRule;
import com.evandev.reliable_remover.mixin.minecraft.accessor.CreativeModeTabsAccessor;
import com.evandev.reliable_remover.platform.Services;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.Stream;

@SuppressWarnings("unused")
public class RuleManager {
    public static final Map<String, Set<String>> EXPANDED_TAGS_CACHE = new ConcurrentHashMap<>();
    static final ThreadLocal<Boolean> SKIP_ADVANCEMENT_CHECK = ThreadLocal.withInitial(() -> false);
    private static final ThreadLocal<Boolean> IN_CHEST_FILL = ThreadLocal.withInitial(() -> false);
    private static final Map<String, List<RemovalRule>> DYNAMIC_RULES = new ConcurrentHashMap<>();
    public static boolean MOD_INIT_PHASE = true;
    private static volatile Map<Action, List<RemovalRule>> RULES_BY_ACTION = new EnumMap<>(Action.class);
    private static volatile Set<String> GLOBALLY_BANNED_ITEMS = ConcurrentHashMap.newKeySet();
    private static volatile Set<String> CNM_CASCADE_REMOVED = ConcurrentHashMap.newKeySet();
    private static volatile Runnable CNM_CASCADE_RECOMPUTE_HOOK = null;
    private static volatile Set<String> TRACKED_ADVANCEMENTS = ConcurrentHashMap.newKeySet();
    private static volatile boolean HAS_ADVANCEMENT_RULES = false;

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

    public static void load() {
        Map<Action, List<RemovalRule>> newRules = new EnumMap<>(Action.class);
        Set<String> newBanned = ConcurrentHashMap.newKeySet();
        for (Action action : Action.values()) newRules.put(action, new ArrayList<>());

        Path configDir = Services.PLATFORM.getConfigDirectory().resolve("reliable_remover");

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

        if (!MOD_INIT_PHASE) {
            validateRules(newRules);
            if (ModConfig.get().blacklistedItems != null) {
                List<String> validBlacklist = new ArrayList<>(ModConfig.get().blacklistedItems);
                validBlacklist.removeIf(itemId -> {
                    if (itemId.startsWith("#")) return false;
                    ResourceLocation id = ResourceLocation.tryParse(itemId);
                    if (id == null) {
                        Constants.LOG.warn("Skipping invalid blacklisted item ID '{}'.", itemId);
                        return true;
                    }
                    if (!BuiltInRegistries.ITEM.containsKey(id)
                            && !BuiltInRegistries.BLOCK.containsKey(id)
                            && !BuiltInRegistries.FLUID.containsKey(id)
                            && !BuiltInRegistries.MOB_EFFECT.containsKey(id)) {
                        Constants.LOG.warn("Skipping invalid blacklisted item/block/fluid/effect ID '{}'.", itemId);
                        return true;
                    }
                    return false;
                });
                ModConfig.get().blacklistedItems = validBlacklist;
            }
        }
        optimizeRules(newRules, newBanned);

        RULES_BY_ACTION = newRules;
        GLOBALLY_BANNED_ITEMS = newBanned;
        GLOBALLY_BANNED_ITEMS.addAll(ModConfig.get().blacklistedItems);
        CNM_CASCADE_REMOVED = ConcurrentHashMap.newKeySet();
        Set<String> newTrackedAdv = ConcurrentHashMap.newKeySet();
        newRules.values().stream().flatMap(List::stream).forEach(r -> {
            if (r.advancements != null && !r.advancements.isEmpty()) {
                newTrackedAdv.addAll(r.advancements);
            }
        });
        TRACKED_ADVANCEMENTS = newTrackedAdv;
        HAS_ADVANCEMENT_RULES = !newTrackedAdv.isEmpty();

        int ruleCount = RULES_BY_ACTION.values().stream().mapToInt(List::size).sum() + GLOBALLY_BANNED_ITEMS.size();
        Constants.LOG.info("Loaded {} reliable remover rules.", ruleCount);
        ReliableRecipesAPI.clearItemReplacements();

        for (List<RemovalRule> rules : RULES_BY_ACTION.values()) {
            for (RemovalRule rule : rules) {
                if (rule.replaceWith != null && !rule.replaceWith.isEmpty()) {
                    if (rule.items != null) {
                        for (String item : rule.items) {
                            ReliableRecipesAPI.registerItemReplacement(item, rule.replaceWith);
                        }
                    }
                }
            }
        }

        if (CNM_CASCADE_RECOMPUTE_HOOK != null) CNM_CASCADE_RECOMPUTE_HOOK.run();

        if (Services.PLATFORM.isPhysicalClient()) {
            try {
                CreativeModeTabsAccessor.setCachedParameters(null);
            } catch (Throwable ignored) {
            }
        }

    }

    public static void registerCnmCascadeRecompute(Runnable hook) {
        CNM_CASCADE_RECOMPUTE_HOOK = hook;
    }

    public static boolean isInChestFill() {
        return IN_CHEST_FILL.get();
    }

    public static void setInChestFill(boolean value) {
        IN_CHEST_FILL.set(value);
    }

    public static boolean isAdvancementTracked(ResourceLocation id) {
        return TRACKED_ADVANCEMENTS.contains(id.toString());
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

    public static boolean isLootBlockedIgnoringAdvancements(ItemStack stack, LootParams context) {
        SKIP_ADVANCEMENT_CHECK.set(true);
        try {
            return isLootBlocked(stack, context);
        } finally {
            SKIP_ADVANCEMENT_CHECK.set(false);
        }
    }

    public static ItemStack getLootReplacementIgnoringAdvancements(ItemStack stack, LootParams context) {
        SKIP_ADVANCEMENT_CHECK.set(true);
        try {
            return getLootReplacement(stack, context);
        } finally {
            SKIP_ADVANCEMENT_CHECK.set(false);
        }
    }

    private static void validateRules(Map<Action, List<RemovalRule>> rulesByAction) {
        for (List<RemovalRule> rules : rulesByAction.values()) {
            for (RemovalRule rule : rules) {
                if (rule.items != null) {
                    rule.items.removeIf(itemId -> {
                        if (itemId.startsWith("#")) return false;

                        ResourceLocation id = ResourceLocation.tryParse(itemId);
                        if (id == null) {
                            Constants.LOG.warn("Skipping invalid ID '{}'.", itemId);
                            return true;
                        }

                        if (rule.action == Action.REMOVE_POTION || rule.action == Action.REMOVE_EFFECT) {
                            if (!BuiltInRegistries.POTION.containsKey(id) && !BuiltInRegistries.MOB_EFFECT.containsKey(id)) {
                                Constants.LOG.warn("Skipping invalid potion/effect ID '{}'.", itemId);
                                return true;
                            }
                        } else if (rule.action == Action.REMOVE_ENCHANTMENT) {
                            return false;
                        } else {
                            if (!BuiltInRegistries.ITEM.containsKey(id)
                                    && !BuiltInRegistries.BLOCK.containsKey(id)
                                    && !BuiltInRegistries.FLUID.containsKey(id)
                                    && !BuiltInRegistries.MOB_EFFECT.containsKey(id)) {
                                Constants.LOG.warn("Skipping invalid item/block/fluid/effect ID '{}'.", itemId);
                                return true;
                            }
                        }
                        return false;
                    });
                }

                if (rule.tags != null) {
                    rule.tags.removeIf(tagId -> {
                        String cleanTagId = tagId.startsWith("#") ? tagId.substring(1) : tagId;
                        ResourceLocation id = ResourceLocation.tryParse(cleanTagId);
                        if (id == null) {
                            Constants.LOG.warn("Skipping invalid tag ID '{}'.", tagId);
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
                (rule.advancements == null || rule.advancements.isEmpty()) &&
                rule.not == null &&
                (rule.replaceWith == null || rule.replaceWith.isEmpty()) &&
                rule.items != null && !rule.items.isEmpty() &&
                rule.items.stream().noneMatch(id -> id.startsWith("#"));
    }

    public static Map<Action, List<RemovalRule>> getRulesByAction() {
        return RULES_BY_ACTION;
    }

    public static void setCnmCascadeRemoved(Set<String> items) {
        CNM_CASCADE_REMOVED = items;
    }

    public static boolean isSkippingAdvancementCheck() {
        return SKIP_ADVANCEMENT_CHECK.get() ||
                com.evandev.reliable_recipes.recipe.RecipeModifier.isModifyingJson() ||
                com.evandev.reliable_recipes.tag.TagModifier.isApplyingTags();
    }

    public static boolean hasAdvancementRules() {
        return HAS_ADVANCEMENT_RULES;
    }

    public static boolean isFluidHidden(String fluidId) {
        if (fluidId == null || fluidId.isEmpty()) return false;
        if (GLOBALLY_BANNED_ITEMS.contains(fluidId)) return true;
        if (CNM_CASCADE_REMOVED.contains(fluidId)) return true;
        return checkRules(null, fluidId, Action.REMOVE, null, null, null, "item");
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

        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        String id = itemId.toString();

        if (GLOBALLY_BANNED_ITEMS.contains(id)) return true;
        if (CNM_CASCADE_REMOVED.contains(id)) return true;

        if (BuiltInRegistries.ITEM.containsKey(itemId)) {
            String dim = level != null ? level.dimension().location().toString() : null;
            if (checkRules(stack, id, Action.REMOVE, dim, holder, null, context)) return true;
        }

        if (id.equals("minecraft:enchanted_book")) {
            if (stack.has(DataComponents.STORED_ENCHANTMENTS)) {
                ItemEnchantments enchantments = stack.get(DataComponents.STORED_ENCHANTMENTS);
                if (enchantments != null && !enchantments.isEmpty()) {
                    boolean allBlocked = true;
                    for (var entry : enchantments.entrySet()) {
                        if (!isEnchantmentBlocked(entry.getKey())) {
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
                String dim = level != null ? level.dimension().location().toString() : null;
                String potionId = contents.potion().flatMap(Holder::unwrapKey).map(key -> key.location().toString()).orElse(null);
                if (potionId != null && checkRules(null, potionId, Action.REMOVE_POTION, dim, holder, null, context))
                    return true;
                for (MobEffectInstance effectInst : contents.getAllEffects()) {
                    Holder<MobEffect> effectHolder = effectInst.getEffect();
                    ResourceLocation loc = BuiltInRegistries.MOB_EFFECT.getKey(effectHolder.value());
                    if (loc != null) {
                        String effectId = loc.toString();
                        if (checkRules(null, effectId, Action.REMOVE_POTION, dim, holder, effectHolder, context))
                            return true;
                    }
                    if (isEffectBlocked(effectHolder, level, holder)) return true;
                }
            }
        }
        return false;
    }

    public static boolean isBlockInteractionBlocked(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (state == null || state.isAir()) return false;
        ResourceLocation loc = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        String id = loc.toString();
        String dim = level != null ? level.dimension().location().toString() : null;
        return checkRules(null, id, Action.REMOVE_INTERACTIONS, dim, entity, state.getBlockHolder(), "block_interaction");
    }

    public static boolean isBlockInteractionBlocked(BlockState state, Level level) {
        return isBlockInteractionBlocked(state, level, null, null);
    }

    public static boolean isPlacementBlocked(ItemStack stack, Level level, Entity entity) {
        if (stack == null || stack.isEmpty()) return false;
        String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        String dim = level != null ? level.dimension().location().toString() : null;
        return checkRules(stack, id, Action.REMOVE_PLACEMENT, dim, entity, null, "placement");
    }

    public static boolean isPlacementBlocked(ItemStack stack, Level level) {
        return isPlacementBlocked(stack, level, null);
    }

    public static boolean isEffectBlocked(Holder<MobEffect> effectHolder, Level level, Entity entity) {
        if (effectHolder == null) return false;
        MobEffect effect = effectHolder.value();
        ResourceLocation loc = BuiltInRegistries.MOB_EFFECT.getKey(effect);
        if (loc == null) return false;
        String id = loc.toString();
        if (GLOBALLY_BANNED_ITEMS.contains(id)) return true;
        if (CNM_CASCADE_REMOVED.contains(id)) return true;
        String dim = level != null ? level.dimension().location().toString() : null;
        if (checkRules(null, id, Action.REMOVE_EFFECT, dim, entity, effectHolder, "effect")) return true;
        return checkRules(null, id, Action.REMOVE, dim, entity, effectHolder, "effect");
    }

    public static boolean isEffectBlocked(Holder<MobEffect> effectHolder, Level level) {
        return isEffectBlocked(effectHolder, level, null);
    }

    public static boolean isEffectCreativeBlocked(Holder<MobEffect> effectHolder, Entity entity) {
        if (effectHolder == null) return false;
        MobEffect effect = effectHolder.value();
        ResourceLocation loc = BuiltInRegistries.MOB_EFFECT.getKey(effect);
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

        if (stack.has(DataComponents.STORED_ENCHANTMENTS)) {
            ItemEnchantments enchantments = stack.get(DataComponents.STORED_ENCHANTMENTS);
            if (enchantments != null) {
                boolean changed = false;
                ItemEnchantments.Mutable validEnchantments = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);

                for (var entry : enchantments.entrySet()) {
                    if (isEnchantmentBlocked(entry.getKey())) {
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
                    if (isEnchantmentBlocked(entry.getKey())) {
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
    }

    public static Holder<Enchantment> getRandomAllowedEnchantment(RegistryAccess registryAccess, RandomSource random, Predicate<Holder<Enchantment>> extraFilter) {
        if (registryAccess == null) return null;
        var registryOpt = registryAccess.registry(Registries.ENCHANTMENT);
        if (registryOpt.isEmpty()) return null;
        var registry = registryOpt.get();
        var candidates = registry.holders()
                .filter(h -> !isEnchantmentBlocked(h))
                .filter(h -> extraFilter == null || extraFilter.test(h))
                .toList();
        if (candidates.isEmpty() && extraFilter != null) {
            candidates = registry.holders().filter(h -> !isEnchantmentBlocked(h)).toList();
        }
        if (candidates.isEmpty()) return null;
        return candidates.get(random.nextInt(candidates.size()));
    }

    public static boolean isAttackBlocked(ItemStack stack, Level level, Entity target) {
        if (stack == null || stack.isEmpty()) return false;
        String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        String dim = level != null ? level.dimension().location().toString() : null;
        return checkRules(stack, id, Action.REMOVE_ATTACKS, dim, target, null, "attack");
    }

    public static boolean isInteractionBlocked(ItemStack stack, Level level, Entity target) {
        if (stack == null || stack.isEmpty()) return false;
        String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        String dim = level != null ? level.dimension().location().toString() : null;
        return checkRules(stack, id, Action.REMOVE_INTERACTIONS, dim, target, null, "interaction");
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
        if (isHidden(stack)) return true;
        if (getLootReplacement(stack, context) != null) return false;
        String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        if (isInChestFill()) {
            if (checkRules(stack, id, Action.REMOVE_CHEST_LOOT, null, null, null, "chest_loot")) return true;
        }
        return checkRules(stack, id, Action.REMOVE_LOOT, null, null, null, "loot");
    }

    public static boolean isInventoryBlocked(ItemStack stack) {
        return isInventoryBlocked(stack, null, null);
    }

    public static boolean isInventoryBlocked(ItemStack stack, Level level, Entity holder) {
        if (stack == null || stack.isEmpty() || !ModConfig.get().removeItemsFromInventories) return false;
        if (isHidden(stack, level, holder)) return true;
        if (getReplacement(stack, Action.REMOVE_INVENTORY, level, holder, "inventory") != null) return false;
        return checkRules(stack, BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(), Action.REMOVE_INVENTORY, level != null ? level.dimension().location().toString() : null, holder, null, "inventory");
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
        return checkRules(stack, BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(), Action.REMOVE_DROPS, level != null ? level.dimension().location().toString() : null, entity, null, "drops");
    }

    public static boolean isEquipmentBlocked(ItemStack stack, Level level, Entity entity) {
        if (stack == null || stack.isEmpty() || !ModConfig.get().removeMobEquipment) return false;
        if (isHidden(stack, level, entity)) return true;
        if (getReplacement(stack, Action.REMOVE_EQUIPMENT, level, entity, "equipment") != null) return false;
        return checkRules(stack, BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(), Action.REMOVE_EQUIPMENT, level != null ? level.dimension().location().toString() : null, entity, null, "equipment");
    }

    public static boolean isStorageBlocked(ItemStack stack, Level level) {
        if (stack == null || stack.isEmpty() || !ModConfig.get().removeItemsFromStorage) return false;
        if (getReplacement(stack, Action.REMOVE_STORAGE, level, null, "storage") != null) return false;
        if (isHidden(stack, level)) return true;
        String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        String dim = level != null ? level.dimension().location().toString() : null;
        return checkRules(stack, id, Action.REMOVE_STORAGE, dim, null, null, "storage");
    }

    public static boolean isHandSwingBlocked(ItemStack stack, Level level) {
        if (stack == null || stack.isEmpty()) return false;
        String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        String dim = level != null ? level.dimension().location().toString() : null;
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

    public static boolean isCreativeBlockedIgnoringAdvancements(ItemStack stack) {
        SKIP_ADVANCEMENT_CHECK.set(true);
        try {
            return isCreativeBlocked(stack);
        } finally {
            SKIP_ADVANCEMENT_CHECK.set(false);
        }
    }

    public static boolean isInfoBlockedIgnoringAdvancements(ItemStack stack) {
        SKIP_ADVANCEMENT_CHECK.set(true);
        try {
            return isInfoBlocked(stack);
        } finally {
            SKIP_ADVANCEMENT_CHECK.set(false);
        }
    }

    public static boolean isEnchantmentBlocked(Holder<Enchantment> enchantment) {
        if (enchantment == null) return false;
        String id = enchantment.unwrapKey().map(key -> key.location().toString()).orElse("");
        return checkRules(null, id, Action.REMOVE_ENCHANTMENT, null, null, enchantment, "enchantment");
    }

    public static ItemStack getReplacement(ItemStack stack, Action action, Level level, Entity holder, String context) {
        if (stack == null || stack.isEmpty()) return null;
        String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        String dim = level != null ? level.dimension().location().toString() : null;
        RemovalRule rule = getMatchingRule(stack, id, action, dim, holder, null, context);
        if (rule == null && action != Action.REMOVE)
            rule = getMatchingRule(stack, id, Action.REMOVE, dim, holder, null, context);

        if (rule != null && rule.replaceWith != null && !rule.replaceWith.isEmpty()) {
            ResourceLocation replacementId = ResourceLocation.tryParse(rule.replaceWith);
            if (replacementId != null && BuiltInRegistries.ITEM.containsKey(replacementId)) {
                ItemStack replacement = new ItemStack(BuiltInRegistries.ITEM.get(replacementId), stack.getCount());
                replacement.applyComponents(stack.getComponentsPatch());
                return replacement;
            }
        }
        return null;
    }

    public static ItemStack getLootReplacement(ItemStack stack, LootParams context) {
        if (stack == null || stack.isEmpty() || !ModConfig.get().removeItemsFromLootChests) return null;
        String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        RemovalRule rule = null;
        if (isInChestFill())
            rule = getMatchingRule(stack, id, Action.REMOVE_CHEST_LOOT, null, null, null, "chest_loot");
        if (rule == null) rule = getMatchingRule(stack, id, Action.REMOVE_LOOT, null, null, null, "loot");
        if (rule == null) rule = getMatchingRule(stack, id, Action.REMOVE, null, null, null, "item");

        if (rule != null && rule.replaceWith != null && !rule.replaceWith.isEmpty()) {
            ResourceLocation replacementId = ResourceLocation.tryParse(rule.replaceWith);
            if (replacementId != null && BuiltInRegistries.ITEM.containsKey(replacementId)) {
                ItemStack replacement = new ItemStack(BuiltInRegistries.ITEM.get(replacementId), stack.getCount());
                replacement.applyComponents(stack.getComponentsPatch());
                return replacement;
            }
        }
        return null;
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

    public static void expandTagRules(RegistryAccess registryAccess) {
        RuleManager.load();
        for (List<RemovalRule> rules : RULES_BY_ACTION.values()) {
            for (RemovalRule rule : rules) {
                rule.expandTags(registryAccess);
            }
        }
        int totalExpandedItems = RULES_BY_ACTION.values().stream()
                .flatMap(List::stream)
                .mapToInt(rule -> rule.items != null ? rule.items.size() : 0)
                .sum();
        Constants.LOG.info("Total items removed: {}", totalExpandedItems + GLOBALLY_BANNED_ITEMS.size());
        if (CNM_CASCADE_RECOMPUTE_HOOK != null) CNM_CASCADE_RECOMPUTE_HOOK.run();
    }
}