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
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
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
    private static final ThreadLocal<Boolean> IN_CHEST_FILL = ThreadLocal.withInitial(() -> false);
    private static final Map<String, List<RemovalRule>> DYNAMIC_RULES = new ConcurrentHashMap<>();
    public static boolean MOD_INIT_PHASE = true;
    private static volatile Map<Action, List<RemovalRule>> RULES_BY_ACTION = new EnumMap<>(Action.class);
    private static volatile Set<String> GLOBALLY_BANNED_ITEMS = ConcurrentHashMap.newKeySet();
    private static volatile Set<String> CNM_CASCADE_REMOVED = ConcurrentHashMap.newKeySet();
    private static volatile Runnable CNM_CASCADE_RECOMPUTE_HOOK = null;

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
        ModConfig.load();
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

        int ruleCount = RULES_BY_ACTION.values().stream().mapToInt(List::size).sum() + GLOBALLY_BANNED_ITEMS.size();
        Map<String, String> replacements = ReliableRecipesAPI.getReplacements();
        if (replacements != null) {
            replacements.clear();
        }

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

    public static void setCnmCascadeRemoved(Set<String> items) {
        CNM_CASCADE_REMOVED = items;
    }

    public static boolean isInChestFill() {
        return IN_CHEST_FILL.get();
    }

    public static void setInChestFill(boolean value) {
        IN_CHEST_FILL.set(value);
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
                rule.not == null &&
                (rule.replaceWith == null || rule.replaceWith.isEmpty()) &&
                rule.items != null && !rule.items.isEmpty() &&
                rule.items.stream().noneMatch(id -> id.startsWith("#"));
    }

    public static Map<Action, List<RemovalRule>> getRulesByAction() {
        return RULES_BY_ACTION;
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
            ListTag listTag = EnchantedBookItem.getEnchantments(stack);
            if (!listTag.isEmpty()) {
                boolean allBlocked = true;
                for (int i = 0; i < listTag.size(); i++) {
                    CompoundTag tag = listTag.getCompound(i);
                    ResourceLocation enchId = ResourceLocation.tryParse(tag.getString("id"));
                    if (enchId != null) {
                        Enchantment ench = BuiltInRegistries.ENCHANTMENT.get(enchId);
                        if (ench != null && !isEnchantmentBlocked(ench)) {
                            allBlocked = false;
                            break;
                        }
                    }
                }
                if (allBlocked) return true;
            }
        }

        Potion potion = PotionUtils.getPotion(stack);
        if (potion != Potions.EMPTY) {
            String potionId = BuiltInRegistries.POTION.getKey(potion).toString();
            String dim = level != null ? level.dimension().location().toString() : null;
            if (checkRules(null, potionId, Action.REMOVE_POTION, dim, holder, null, context)) {
                return true;
            }
        }

        for (MobEffectInstance effectInst : PotionUtils.getMobEffects(stack)) {
            MobEffect effect = effectInst.getEffect();
            ResourceLocation loc = BuiltInRegistries.MOB_EFFECT.getKey(effect);
            if (loc != null) {
                String effectId = loc.toString();
                String dim = level != null ? level.dimension().location().toString() : null;
                if (checkRules(null, effectId, Action.REMOVE_POTION, dim, holder, null, context)) {
                    return true;
                }
            }
            if (isEffectBlocked(effect, level, holder)) return true;
        }

        return false;
    }

    public static boolean isBlockInteractionBlocked(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (state == null || state.isAir()) return false;
        ResourceLocation loc = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        String id = loc.toString();
        String dim = level != null ? level.dimension().location().toString() : null;
        return checkRules(null, id, Action.REMOVE_INTERACTIONS, dim, entity, state.getBlock().builtInRegistryHolder(), "block_interaction");
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

    public static boolean isEffectBlocked(MobEffect effect, Level level, Entity entity) {
        if (effect == null) return false;
        ResourceLocation loc = BuiltInRegistries.MOB_EFFECT.getKey(effect);
        if (loc == null) return false;
        String id = loc.toString();
        if (GLOBALLY_BANNED_ITEMS.contains(id)) return true;
        if (CNM_CASCADE_REMOVED.contains(id)) return true;
        String dim = level != null ? level.dimension().location().toString() : null;
        Holder<MobEffect> holder = BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect);
        if (checkRules(null, id, Action.REMOVE_EFFECT, dim, entity, holder, "effect")) return true;
        return checkRules(null, id, Action.REMOVE, dim, entity, holder, "effect");
    }

    public static boolean isEffectBlocked(MobEffect effect, Level level) {
        return isEffectBlocked(effect, level, null);
    }

    public static boolean isEffectCreativeBlocked(MobEffect effect) {
        if (effect == null) return false;
        ResourceLocation loc = BuiltInRegistries.MOB_EFFECT.getKey(effect);
        if (loc == null) return false;
        String id = loc.toString();
        if (GLOBALLY_BANNED_ITEMS.contains(id)) return true;
        if (CNM_CASCADE_REMOVED.contains(id)) return true;
        Holder<MobEffect> holder = BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect);
        if (checkRules(null, id, Action.REMOVE_CREATIVE, null, null, holder, "creative")) return true;
        if (checkRules(null, id, Action.REMOVE_EFFECT, null, null, holder, "effect")) return true;
        return checkRules(null, id, Action.REMOVE, null, null, holder, "effect");
    }

    public static boolean isEffectCreativeBlocked(MobEffect effect, Entity entity) {
        return isEffectCreativeBlocked(effect);
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

        if (stack.is(Items.ENCHANTED_BOOK)) {
            ListTag listTag = EnchantedBookItem.getEnchantments(stack);
            if (!listTag.isEmpty()) {
                boolean changed = false;
                ListTag validEnchantments = new ListTag();

                for (int i = 0; i < listTag.size(); i++) {
                    CompoundTag tag = listTag.getCompound(i);
                    ResourceLocation enchId = ResourceLocation.tryParse(tag.getString("id"));
                    if (enchId != null) {
                        Enchantment ench = BuiltInRegistries.ENCHANTMENT.get(enchId);
                        if (isEnchantmentBlocked(ench)) {
                            changed = true;
                        } else {
                            validEnchantments.add(tag);
                        }
                    } else {
                        validEnchantments.add(tag);
                    }
                }

                if (changed) {
                    if (validEnchantments.isEmpty()) {
                        Enchantment rerolled = getRandomAllowedEnchantment(registryAccess, random, null);
                        if (rerolled != null) {
                            int level = Mth.nextInt(random, rerolled.getMinLevel(), rerolled.getMaxLevel());
                            EnchantedBookItem.addEnchantment(stack, new EnchantmentInstance(rerolled, level));
                        } else {
                            stack.removeTagKey(EnchantedBookItem.TAG_STORED_ENCHANTMENTS);
                        }
                    } else {
                        stack.getOrCreateTag().put(EnchantedBookItem.TAG_STORED_ENCHANTMENTS, validEnchantments);
                    }
                }
            }
        }

        if (stack.isEnchanted()) {
            Map<Enchantment, Integer> enchantments = EnchantmentHelper.getEnchantments(stack);
            if (!enchantments.isEmpty()) {
                boolean changed = false;
                Map<Enchantment, Integer> validEnchantments = new LinkedHashMap<>();

                for (Map.Entry<Enchantment, Integer> entry : enchantments.entrySet()) {
                    if (isEnchantmentBlocked(entry.getKey())) {
                        changed = true;
                    } else {
                        validEnchantments.put(entry.getKey(), entry.getValue());
                    }
                }

                if (changed) {
                    EnchantmentHelper.setEnchantments(validEnchantments, stack);
                }
            }
        }
    }

    public static Enchantment getRandomAllowedEnchantment(RegistryAccess registryAccess, RandomSource random, Predicate<Enchantment> extraFilter) {
        var registry = BuiltInRegistries.ENCHANTMENT;
        var candidates = registry.stream()
                .filter(ench -> !isEnchantmentBlocked(ench))
                .filter(ench -> extraFilter == null || extraFilter.test(ench))
                .toList();
        if (candidates.isEmpty() && extraFilter != null) {
            candidates = registry.stream().filter(ench -> !isEnchantmentBlocked(ench)).toList();
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
        if (stack == null || stack.isEmpty() || !ModConfig.get().removeItemsFromCreativeTabs) return false;
        if (isHidden(stack)) return true;
        if (getReplacement(stack, Action.REMOVE_CREATIVE, null, null, "creative") != null) return false;
        return checkRules(stack, BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(), Action.REMOVE_CREATIVE, null, null, null, "creative");
    }

    public static boolean isCreativeBlocked(ItemStack stack, Entity player) {
        return isCreativeBlocked(stack);
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
        if (stack == null || stack.isEmpty()) return false;
        String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        return checkRules(stack, id, Action.REMOVE_INFO, null, null, null, "info");
    }

    public static boolean isInfoBlocked(ItemStack stack, Entity player) {
        return isInfoBlocked(stack);
    }

    public static boolean isEnchantmentBlocked(Enchantment enchantment) {
        if (enchantment == null) return false;
        ResourceLocation key = BuiltInRegistries.ENCHANTMENT.getKey(enchantment);
        if (key == null) return false;
        String id = key.toString();
        Holder<Enchantment> enchHolder = BuiltInRegistries.ENCHANTMENT.wrapAsHolder(enchantment);
        return checkRules(null, id, Action.REMOVE_ENCHANTMENT, null, null, enchHolder, "enchantment");
    }

    public static boolean isEnchantmentBlocked(Holder<Enchantment> enchantment) {
        if (enchantment == null) return false;
        return isEnchantmentBlocked(enchantment.value());
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
                if (stack.hasTag() && stack.getTag() != null) {
                    replacement.setTag(stack.getTag().copy());
                }
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
                if (stack.hasTag() && stack.getTag() != null) {
                    replacement.setTag(stack.getTag().copy());
                }
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