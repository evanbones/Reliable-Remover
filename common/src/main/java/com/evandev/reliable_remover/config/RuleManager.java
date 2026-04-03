package com.evandev.reliable_remover.config;

import com.evandev.reliable_remover.Constants;
import com.evandev.reliable_remover.data.Action;
import com.evandev.reliable_remover.data.RemovalRule;
import com.evandev.reliable_remover.platform.Services;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

public class RuleManager {
    private static volatile Map<Action, List<RemovalRule>> RULES_BY_ACTION = new EnumMap<>(Action.class);
    private static volatile Set<String> GLOBALLY_BANNED_ITEMS = ConcurrentHashMap.newKeySet();

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

        validateRules(newRules);
        optimizeRules(newRules, newBanned);

        RULES_BY_ACTION = newRules;
        GLOBALLY_BANNED_ITEMS = newBanned;
        GLOBALLY_BANNED_ITEMS.addAll(ModConfig.get().blacklistedItems);

        int ruleCount = RULES_BY_ACTION.values().stream().mapToInt(List::size).sum() + GLOBALLY_BANNED_ITEMS.size();
        Constants.LOG.info("Loaded {} reliable remover rules.", ruleCount);
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
            Constants.LOG.info("Created example config at config/reliable_remover/example_rules.json.disabled");
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
                            Constants.LOG.warn("Reliable Remover: Skipping completely invalid ID '{}'.", itemId);
                            return true;
                        }

                        if (rule.action == Action.REMOVE_POTION) {
                            if (!BuiltInRegistries.POTION.containsKey(id)) {
                                Constants.LOG.warn("Reliable Remover: Skipping invalid potion ID '{}'.", itemId);
                                return true;
                            }
                        } else if (rule.action == Action.REMOVE_ENCHANTMENT) {
                            return false;
                        } else {
                            if (!BuiltInRegistries.ITEM.containsKey(id)) {
                                Constants.LOG.warn("Reliable Remover: Skipping invalid item ID '{}'.", itemId);
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
                (rule.replaceWith == null || rule.replaceWith.isEmpty()) &&
                rule.items != null && !rule.items.isEmpty() &&
                rule.items.stream().noneMatch(id -> id.startsWith("#"));
    }

    public static Map<Action, List<RemovalRule>> getRulesByAction() {
        return RULES_BY_ACTION;
    }

    public static boolean isHidden(ItemStack stack) {
        return isHidden(stack, null, null, "item");
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

        if (BuiltInRegistries.ITEM.containsKey(itemId)) {
            String dim = level != null ? level.dimension().location().toString() : null;
            if (checkRules(stack, id, Action.REMOVE, dim, holder, null, context)) {
                if (dim == null && holder == null && (context == null || context.equals("item"))) {
                    GLOBALLY_BANNED_ITEMS.add(id);
                }
                return true;
            }
        }

        if (stack.isEnchanted() || id.equals("minecraft:enchanted_book")) {
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
                    if (id.equals("minecraft:enchanted_book")) return true;
                    EnchantmentHelper.setEnchantments(validEnchantments, stack);
                }
            }
        }

        Potion potion = PotionUtils.getPotion(stack);
        if (potion != Potions.EMPTY) {
            String potionId = BuiltInRegistries.POTION.getKey(potion).toString();
            return checkRules(null, potionId, Action.REMOVE_POTION, null, holder, null, context);
        }

        return false;
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
        if (stack == null || stack.isEmpty()) return false;
        if (isHidden(stack)) return true;
        String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        return checkRules(stack, id, Action.REMOVE_TRADE, null, null, null, "trade");
    }

    public static boolean isLootBlocked(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        if (isHidden(stack)) return true;
        String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        return checkRules(stack, id, Action.REMOVE_LOOT, null, null, null, "loot");
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

    public static boolean isEnchantmentBlocked(Enchantment enchantment) {
        String id = BuiltInRegistries.ENCHANTMENT.getKey(enchantment).toString();
        Holder<Enchantment> enchHolder = BuiltInRegistries.ENCHANTMENT.wrapAsHolder(enchantment);
        return checkRules(null, id, Action.REMOVE_ENCHANTMENT, null, null, enchHolder, "enchantment");
    }

    private static boolean checkRules(ItemStack stack, String itemId, Action action, String dimension, Entity target, Holder<?> registryHolder, String context) {
        String entityId = target != null ? BuiltInRegistries.ENTITY_TYPE.getKey(target.getType()).toString() : null;

        List<RemovalRule> rules = RULES_BY_ACTION.get(action);
        if (rules == null || rules.isEmpty()) return false;

        for (RemovalRule rule : rules) {
            if (rule.action == action) {
                if (rule.matches(stack, itemId, dimension, entityId, registryHolder, context)) return true;
            }
        }
        return false;
    }
}