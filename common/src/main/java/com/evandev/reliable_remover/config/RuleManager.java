package com.evandev.reliable_remover.config;

import com.evandev.reliable_remover.Constants;
import com.evandev.reliable_remover.data.Action;
import com.evandev.reliable_remover.data.RemovalRule;
import com.evandev.reliable_remover.platform.Services;
import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;

import java.io.FileReader;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

public class RuleManager {
    private static final Gson GSON = new GsonBuilder()
            .setLenient()
            .registerTypeAdapter(new TypeToken<Set<String>>() {
            }.getType(), new StringOrSetDeserializer())
            .registerTypeAdapter(new TypeToken<List<String>>() {
            }.getType(), new StringOrListDeserializer())
            .create();

    private static volatile Map<Action, List<RemovalRule>> RULES_BY_ACTION = new EnumMap<>(Action.class);
    private static volatile Set<String> GLOBALLY_BANNED_ITEMS = new HashSet<>();

    public static void load() {
        Map<Action, List<RemovalRule>> newRules = new EnumMap<>(Action.class);
        Set<String> newBanned = new HashSet<>();
        for (Action action : Action.values()) {
            newRules.put(action, new ArrayList<>());
        }

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
                files.forEach(path -> parseFile(path, newRules));
            }
        } catch (Exception e) {
            Constants.LOG.error("Failed to load removal rules", e);
        }

        if (!hasFiles) generateDefaultConfig(configDir);

        validateRules(newRules);
        optimizeRules(newRules, newBanned);

        // Atomically swap the references
        RULES_BY_ACTION = newRules;
        GLOBALLY_BANNED_ITEMS = newBanned;

        int ruleCount = RULES_BY_ACTION.values().stream().mapToInt(List::size).sum() + GLOBALLY_BANNED_ITEMS.size();
        Constants.LOG.info("Loaded {} reliable remover rules.", ruleCount);
        logRemovedItems();
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
                            if (!BuiltInRegistries.ENCHANTMENT.containsKey(id)) {
                                Constants.LOG.warn("Reliable Remover: Skipping invalid enchantment ID '{}'.", itemId);
                                return true;
                            }
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
                rule.not == null &&
                rule.items != null && !rule.items.isEmpty();
    }

    private static void logRemovedItems() {
        long count = BuiltInRegistries.ITEM.keySet().stream()
                .filter(location -> {
                    String id = location.toString();
                    if (GLOBALLY_BANNED_ITEMS.contains(id)) return true;

                    return checkRules(null, id, Action.REMOVE, null, null);
                })
                .count();

        Constants.LOG.info("Reliable Remover: Removed {} items from the game.", count);
    }

    private static void parseFile(Path path, Map<Action, List<RemovalRule>> rulesByAction) {
        try (FileReader fileReader = new FileReader(path.toFile())) {
            JsonReader reader = new JsonReader(fileReader);
            reader.setLenient(true);

            while (reader.peek() != com.google.gson.stream.JsonToken.END_DOCUMENT) {
                JsonElement json = JsonParser.parseReader(reader);

                if (json.isJsonArray()) {
                    for (JsonElement e : json.getAsJsonArray()) {
                        addRule(GSON.fromJson(e, RemovalRule.class), rulesByAction);
                    }
                } else if (json.isJsonObject()) {
                    addRule(GSON.fromJson(json, RemovalRule.class), rulesByAction);
                }
            }
        } catch (Exception e) {
            Constants.LOG.error("Error parsing file: {}", path, e);
        }
    }

    private static void addRule(RemovalRule rule, Map<Action, List<RemovalRule>> rulesByAction) {
        if (rule.action == null) rule.action = Action.REMOVE;
        rulesByAction.computeIfAbsent(rule.action, k -> new ArrayList<>()).add(rule);
    }

    public static boolean isHidden(ItemStack stack) {
        return isHidden(stack, null, null);
    }

    public static boolean isHidden(ItemStack stack, Level level) {
        return isHidden(stack, level, null);
    }

    public static boolean isHidden(ItemStack stack, Level level, Entity holder) {
        if (stack == null || stack.isEmpty()) return false;

        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        String id = itemId.toString();

        if (GLOBALLY_BANNED_ITEMS.contains(id)) return true;

        if (BuiltInRegistries.ITEM.containsKey(itemId)) {
            String dim = level != null ? level.dimension().location().toString() : null;
            if (checkRules(stack, id, Action.REMOVE, dim, holder)) {
                return true;
            }
        }

        Map<Enchantment, Integer> enchants = EnchantmentHelper.getEnchantments(stack);
        for (Enchantment enc : enchants.keySet()) {
            if (isEnchantmentBlocked(enc)) return true;
        }

        Potion potion = PotionUtils.getPotion(stack);
        ResourceLocation potionId = BuiltInRegistries.POTION.getKey(potion);
        return checkRules(null, potionId.toString(), Action.REMOVE_POTION, null, holder);
    }

    public static boolean isAttackBlocked(ItemStack stack, Level level, Entity target) {
        if (stack == null || stack.isEmpty()) return false;

        String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        String dim = level != null ? level.dimension().location().toString() : null;
        return checkRules(stack, id, Action.REMOVE_ATTACKS, dim, target);
    }

    public static boolean isInteractionBlocked(ItemStack stack, Level level, Entity target) {
        if (stack == null || stack.isEmpty()) return false;

        String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        String dim = level != null ? level.dimension().location().toString() : null;
        return checkRules(stack, id, Action.REMOVE_INTERACTIONS, dim, target);
    }

    public static boolean isTradeBlocked(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        if (isHidden(stack)) return true;

        String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        return checkRules(stack, id, Action.REMOVE_TRADE, null, null);
    }

    public static boolean isLootBlocked(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        if (isHidden(stack)) return true;

        String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        return checkRules(stack, id, Action.REMOVE_LOOT, null, null);
    }

    public static boolean isHandSwingBlocked(ItemStack stack, Level level) {
        if (stack == null || stack.isEmpty()) return false;

        String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        String dim = level != null ? level.dimension().location().toString() : null;
        return checkRules(stack, id, Action.REMOVE_HAND_SWING, dim, null);
    }

    private static boolean checkRules(ItemStack stack, String itemId, Action action, String dimension, Entity target) {
        String entityId = target != null ? BuiltInRegistries.ENTITY_TYPE.getKey(target.getType()).toString() : null;

        List<RemovalRule> rules = RULES_BY_ACTION.get(action);
        if (rules == null || rules.isEmpty()) return false;

        for (RemovalRule rule : rules) {
            if (rule.action == action) {
                if (stack != null) {
                    if (rule.matches(stack, itemId, dimension, entityId)) return true;
                } else {
                    if (rule.matches(itemId, dimension, entityId)) return true;
                }
            }
        }
        return false;
    }

    public static boolean isEnchantmentBlocked(Enchantment enchantment) {
        ResourceLocation id = BuiltInRegistries.ENCHANTMENT.getKey(enchantment);
        if (id != null) {
            return checkRules(null, id.toString(), Action.REMOVE_ENCHANTMENT, null, null);
        }
        return false;
    }

    private static class StringOrSetDeserializer implements JsonDeserializer<Set<String>> {
        @Override
        public Set<String> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            Set<String> set = new HashSet<>();
            if (json.isJsonArray()) {
                for (JsonElement e : json.getAsJsonArray()) {
                    set.add(e.getAsString());
                }
            } else if (json.isJsonPrimitive()) {
                set.add(json.getAsString());
            }
            return set;
        }
    }

    private static class StringOrListDeserializer implements JsonDeserializer<List<String>> {
        @Override
        public List<String> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            List<String> list = new ArrayList<>();
            if (json.isJsonArray()) {
                for (JsonElement e : json.getAsJsonArray()) {
                    list.add(e.getAsString());
                }
            } else if (json.isJsonPrimitive()) {
                list.add(json.getAsString());
            }
            return list;
        }
    }
}