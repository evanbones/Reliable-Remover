package com.evandev.reliable_remover.config;

import com.evandev.reliable_recipes.api.ReliableRecipesAPI;
import com.evandev.reliable_remover.Constants;
import com.evandev.reliable_remover.data.Action;
import com.evandev.reliable_remover.data.RemovalRule;
import com.evandev.reliable_remover.platform.Services;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

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

        Identifier itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        String id = itemId.toString();

        if (GLOBALLY_BANNED_ITEMS.contains(id)) return true;

        if (BuiltInRegistries.ITEM.containsKey(itemId)) {
            String dim = level != null ? level.dimension().identifier().toString() : null;
            if (checkRules(stack, id, Action.REMOVE, dim, holder, null, context)) return true;
        }

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
                    if (id.equals("minecraft:enchanted_book")) return true;
                    stack.set(DataComponents.STORED_ENCHANTMENTS, validEnchantments.toImmutable());
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
                    if (id.equals("minecraft:enchanted_book")) return true;
                    stack.set(DataComponents.ENCHANTMENTS, validEnchantments.toImmutable());
                }
            }
        }

        if (stack.has(DataComponents.POTION_CONTENTS)) {
            PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
            if (contents != null) {
                String potionId = contents.potion().flatMap(Holder::unwrapKey).map(key -> key.identifier().toString()).orElse(null);
                return potionId != null && checkRules(null, potionId, Action.REMOVE_POTION, null, holder, null, context);
            }
        }
        return false;
    }

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

        if (context != null && context.contextMap().has(LootContextParams.BLOCK_ENTITY)) {
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
        return checkRules(stack, BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(), Action.REMOVE_INVENTORY, level != null ? level.dimension().identifier().toString() : null, holder, null, "inventory");
    }

    public static boolean isCreativeBlocked(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !ModConfig.get().removeItemsFromCreativeTabs) return false;
        if (isHidden(stack)) return true;
        if (getReplacement(stack, Action.REMOVE_CREATIVE, null, null, "creative") != null) return false;
        return checkRules(stack, BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(), Action.REMOVE_CREATIVE, null, null, null, "creative");
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

    public static boolean isRecipeBlocked(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !ModConfig.get().removeRecipes) return false;
        if (getReplacement(stack, Action.REMOVE_RECIPE, null, null, "recipe") != null) return false;
        if (isHidden(stack)) return true;
        String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        return checkRules(stack, id, Action.REMOVE_RECIPE, null, null, null, "recipe");
    }

    public static boolean isRecipeBlocked(Recipe<?> recipe) {
        if (!ModConfig.get().removeRecipes) return false;
        List<ItemStack> outputs = ReliableRecipesAPI.getRecipeResults(recipe);
        if (outputs.isEmpty()) return false;
        for (ItemStack stack : outputs) {
            if (!stack.isEmpty() && !isRecipeBlocked(stack)) return false;
        }
        return true;
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
        if (stack == null || stack.isEmpty()) return false;
        String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        return checkRules(stack, id, Action.REMOVE_INFO, null, null, null, "info");
    }

    public static boolean isEnchantmentBlocked(Holder<Enchantment> enchantment) {
        return enchantment.unwrapKey()
                .map(key -> key.identifier().toString())
                .map(id -> checkRules(null, id, Action.REMOVE_ENCHANTMENT, null, null, enchantment, "enchantment"))
                .orElse(false);
    }

    public static ItemStack getReplacement(ItemStack stack, Action action, Level level, Entity holder, String context) {
        if (stack == null || stack.isEmpty()) return null;
        String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        String dim = level != null ? level.dimension().identifier().toString() : null;
        RemovalRule rule = getMatchingRule(stack, id, action, dim, holder, null, context);
        if (rule == null && action != Action.REMOVE)
            rule = getMatchingRule(stack, id, Action.REMOVE, dim, holder, null, context);

        if (rule != null && rule.replaceWith != null && !rule.replaceWith.isEmpty()) {
            Identifier replacementId = Identifier.tryParse(rule.replaceWith);
            if (replacementId != null) {
                var itemOpt = BuiltInRegistries.ITEM.get(replacementId);
                if (itemOpt.isPresent()) {
                    ItemStack replacement = new ItemStack(itemOpt.get(), stack.getCount());
                    replacement.applyComponents(stack.getComponentsPatch());
                    return replacement;
                }
            }
        }
        return null;
    }

    public static ItemStack getLootReplacement(ItemStack stack, LootParams context) {
        if (stack == null || stack.isEmpty() || !ModConfig.get().removeItemsFromLootChests) return null;
        String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        RemovalRule rule = null;

        if (context != null && context.contextMap().has(LootContextParams.BLOCK_ENTITY))
            rule = getMatchingRule(stack, id, Action.REMOVE_CHEST_LOOT, null, null, null, "chest_loot");

        if (rule == null) rule = getMatchingRule(stack, id, Action.REMOVE_LOOT, null, null, null, "loot");
        if (rule == null) rule = getMatchingRule(stack, id, Action.REMOVE, null, null, null, "item");

        if (rule != null && rule.replaceWith != null && !rule.replaceWith.isEmpty()) {
            Identifier replacementId = Identifier.tryParse(rule.replaceWith);
            if (replacementId != null) {
                var itemOpt = BuiltInRegistries.ITEM.get(replacementId);
                if (itemOpt.isPresent()) {
                    ItemStack replacement = new ItemStack(itemOpt.get(), stack.getCount());
                    replacement.applyComponents(stack.getComponentsPatch());
                    return replacement;
                }
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
            if (rule.action == action && rule.matches(stack, itemId, dimension, entityId, registryHolder, context))
                return rule;
        }
        return null;
    }
}