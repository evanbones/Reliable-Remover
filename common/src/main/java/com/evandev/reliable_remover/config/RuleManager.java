package com.evandev.reliable_remover.config;

import com.evandev.reliable_remover.Constants;
import com.evandev.reliable_remover.data.RemovalRule;
import com.evandev.reliable_remover.platform.Services;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.io.FileReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class RuleManager {
    private static final Gson GSON = new GsonBuilder().setLenient().create();
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
                        "filter": {
                            "items": [
                                "examplemod:item1",
                                "examplemod:item2"
                            ]
                        }
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
            if (rule.filter != null && rule.filter.items != null) {
                rule.filter.items.removeIf(itemId -> {
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

    private static void logRemovedItems() {
        List<String> removedItems = BuiltInRegistries.ITEM.entrySet().stream()
                .filter(entry -> isHidden(entry.getValue().getDefaultInstance()))
                .map(entry -> entry.getKey().identifier().toString())
                .collect(Collectors.toList());

        if (!removedItems.isEmpty()) {
            Constants.LOG.info("Reliable Remover: Removed {} items from the game.", removedItems.size());
            Constants.LOG.debug("Removed items: {}", String.join(", ", removedItems));
        }
    }

    private static void parseFile(Path path) {
        try (FileReader reader = new FileReader(path.toFile())) {
            JsonElement json = JsonParser.parseReader(reader);
            if (json.isJsonArray()) {
                for (JsonElement e : json.getAsJsonArray()) RULES.add(GSON.fromJson(e, RemovalRule.class));
            } else if (json.isJsonObject()) {
                RULES.add(GSON.fromJson(json, RemovalRule.class));
            }
        } catch (Exception e) {
            Constants.LOG.error("Error parsing file: {}", path, e);
        }
    }

    public static boolean isHidden(ItemStack stack) {
        return isHidden(stack, null);
    }

    public static boolean isHidden(ItemStack stack, Level level) {
        if (stack == null || stack.isEmpty()) return false;

        Identifier itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (!BuiltInRegistries.ITEM.containsKey(itemId)) {
            return false;
        }

        String id = itemId.toString();
        String dim = level != null ? level.dimension().identifier().toString() : null;
        return checkRules(stack, id, RemovalRule.Action.REMOVE, dim, null);
    }

    public static boolean isHidden(String itemId) {
        return checkRules(null, itemId, RemovalRule.Action.REMOVE, null, null);
    }

    public static boolean isAttackBlocked(ItemStack stack, Level level, Entity target) {
        if (stack == null || stack.isEmpty()) return false;
        String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        String dim = level != null ? level.dimension().identifier().toString() : null;
        return checkRules(stack, id, RemovalRule.Action.REMOVE_ATTACKS, dim, target) || isHidden(stack, level);
    }

    public static boolean isInteractionBlocked(ItemStack stack, Level level, Entity target) {
        if (stack == null || stack.isEmpty()) return false;
        String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        String dim = level != null ? level.dimension().identifier().toString() : null;
        return checkRules(stack, id, RemovalRule.Action.REMOVE_INTERACTIONS, dim, target) || isHidden(stack, level);
    }

    private static boolean checkRules(ItemStack stack, String itemId, RemovalRule.Action action, String dimension, Entity target) {
        String entityId = target != null ? BuiltInRegistries.ENTITY_TYPE.getKey(target.getType()).toString() : null;

        for (RemovalRule rule : RULES) {
            if (rule.action == action) {
                if (rule.filter != null) {
                    if (stack != null) {
                        if (rule.filter.matches(stack, itemId, dimension, entityId)) return true;
                    } else {
                        if (rule.filter.matches(itemId, dimension, entityId)) return true;
                    }
                }
            }
        }
        return false;
    }
}