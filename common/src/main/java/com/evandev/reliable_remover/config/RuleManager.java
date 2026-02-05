package com.evandev.reliable_remover.config;

import com.evandev.reliable_remover.Constants;
import com.evandev.reliable_remover.data.RemovalRule;
import com.evandev.reliable_remover.platform.Services;
import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;

import java.io.FileReader;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class RuleManager {
    private static final Gson GSON = new GsonBuilder()
            .setStrictness(Strictness.LENIENT)
            .registerTypeAdapter(new TypeToken<Set<String>>() {
            }.getType(), new StringOrSetDeserializer())
            .registerTypeAdapter(new TypeToken<List<String>>() {
            }.getType(), new StringOrListDeserializer())
            .create();

    private static final List<RemovalRule> RULES = new ArrayList<>();

    public static void load() {
        RULES.clear();
        Path configDir = Services.PLATFORM.getConfigDirectory().resolve("reliable_remover");

        if (!Files.exists(configDir)) {
            try {
                Files.createDirectories(configDir);
            } catch (Exception ignored) {
            }
        }

        boolean hasFiles = false;
        try (Stream<Path> paths = Files.walk(configDir)) {
            List<Path> files = paths.filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".json"))
                    .toList();

            if (!files.isEmpty()) {
                hasFiles = true;
                files.forEach(RuleManager::parseFile);
            }
        } catch (Exception e) {
            Constants.LOG.error("Failed to load removal rules", e);
        }

        if (!hasFiles) {
            generateDefaultConfig(configDir);
        }

        Constants.LOG.info("Loaded {} reliable remover rules.", RULES.size());
        validateRules();
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

    private static void validateRules() {
        for (RemovalRule rule : RULES) {
            if (rule.items != null) {
                if (rule.action == RemovalRule.Action.REMOVE ||
                        rule.action == RemovalRule.Action.REMOVE_ATTACKS ||
                        rule.action == RemovalRule.Action.REMOVE_INTERACTIONS) {

                    rule.items.removeIf(itemId -> {
                        Identifier id = Identifier.tryParse(itemId);
                        if (id == null || !BuiltInRegistries.ITEM.containsKey(id)) {
                            Constants.LOG.warn("Reliable Remover: Skipping invalid item ID '{}'. This item does not exist.", itemId);
                            return true;
                        }
                        return false;
                    });
                }
            }
        }
    }

    private static void logRemovedItems() {
        List<String> removedItems = BuiltInRegistries.ITEM.entrySet().stream()
                .filter(entry -> isHidden(entry.getValue()))
                .map(entry -> entry.getKey().identifier().toString())
                .collect(Collectors.toList());

        if (!removedItems.isEmpty()) {
            Constants.LOG.info("Reliable Remover: Removed {} items from the game.", removedItems.size());
            Constants.LOG.debug("Removed items: {}", String.join(", ", removedItems));
        }
    }

    private static void parseFile(Path path) {
        try (FileReader fileReader = new FileReader(path.toFile())) {
            JsonReader reader = new JsonReader(fileReader);
            reader.setLenient(true);

            while (reader.peek() != com.google.gson.stream.JsonToken.END_DOCUMENT) {
                JsonElement json = JsonParser.parseReader(reader);

                if (json.isJsonArray()) {
                    for (JsonElement e : json.getAsJsonArray()) {
                        RemovalRule rule = GSON.fromJson(e, RemovalRule.class);
                        rule.mergeLegacy();
                        RULES.add(rule);
                    }
                } else if (json.isJsonObject()) {
                    RemovalRule rule = GSON.fromJson(json, RemovalRule.class);
                    rule.mergeLegacy();
                    RULES.add(rule);
                }
            }
        } catch (Exception e) {
            Constants.LOG.error("Error parsing file: {}", path, e);
        }
    }

    public static boolean isHidden(Item item) {
        if (isEmpty(item)) return false;
        return isHidden(new ItemStack(item), null);
    }

    public static boolean isHidden(ItemStack stack) {
        if (isEmpty(stack)) return false;
        return isHidden(stack, null);
    }

    public static boolean isHidden(ItemStack stack, Level level) {
        Identifier itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (BuiltInRegistries.ITEM.containsKey(itemId)) {
            String id = itemId.toString();
            String dim = level != null ? level.dimension().identifier().toString() : null;
            if (checkRules(stack, id, RemovalRule.Action.REMOVE, dim, null)) {
                return true;
            }
        }

        if (stack.get(DataComponents.STORED_ENCHANTMENTS) != null) {
            if (isEnchantmentBlocked(stack.get(DataComponents.STORED_ENCHANTMENTS))) return true;
        }
        if (stack.get(DataComponents.ENCHANTMENTS) != null) {
            if (isEnchantmentBlocked(stack.get(DataComponents.ENCHANTMENTS))) return true;
        }

        if (stack.get(DataComponents.POTION_CONTENTS) != null) {
            PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
            if (contents != null) {
                String potionId = contents.potion()
                        .flatMap(Holder::unwrapKey)
                        .map(key -> key.identifier().toString())
                        .orElse(null);

                return potionId != null && checkRules(null, potionId, RemovalRule.Action.REMOVE_POTION, null, null);
            }
        }

        return false;
    }

    private static boolean isEnchantmentBlocked(ItemEnchantments enchantments) {
        if (enchantments == null) return false;
        for (var entry : enchantments.entrySet()) {
            String id = entry.getKey()
                    .unwrapKey()
                    .map(key -> key.identifier().toString())
                    .orElse(null);

            if (id != null && checkRules(null, id, RemovalRule.Action.REMOVE_ENCHANTMENT, null, null)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isHidden(String itemId) {
        return checkRules(null, itemId, RemovalRule.Action.REMOVE, null, null);
    }

    public static boolean isAttackBlocked(ItemStack stack, Level level, Entity target) {
        if (isEmpty(stack)) return false;
        String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        String dim = level != null ? level.dimension().identifier().toString() : null;
        return checkRules(stack, id, RemovalRule.Action.REMOVE_ATTACKS, dim, target) || isHidden(stack, level);
    }

    private static boolean isEmpty(Item item) {
        return item == null || item == Items.AIR;
    }

	private static boolean isEmpty(ItemStackTemplate template) {
		return template == null || isEmpty(getItem(template));
	}

    private static boolean isEmpty(ItemStack stack) {
        return stack == null || stack.isEmpty() || isEmpty(stack.getItem());
    }

    private static Item getItem(ItemStackTemplate stack) {
        return stack.item().value();
    }

    public static boolean isInteractionBlocked(ItemStack stack, Level level, Entity target) {
        if (isEmpty(stack)) return false;
        String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        String dim = level != null ? level.dimension().identifier().toString() : null;
        return checkRules(stack, id, RemovalRule.Action.REMOVE_INTERACTIONS, dim, target) || isHidden(stack, level);
    }

    private static boolean checkRules(ItemStack stack, String itemId, RemovalRule.Action action, String dimension, Entity target) {
        String entityId = target != null ? BuiltInRegistries.ENTITY_TYPE.getKey(target.getType()).toString() : null;

        for (RemovalRule rule : RULES) {
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

    public static boolean isEnchantmentBlocked(Holder<Enchantment> enchantment) {
        return enchantment.unwrapKey()
                .map(key -> key.identifier().toString())
                .map(id -> checkRules(null, id, RemovalRule.Action.REMOVE_ENCHANTMENT, null, null))
                .orElse(false);
    }
}