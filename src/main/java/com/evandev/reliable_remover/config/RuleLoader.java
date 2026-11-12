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
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

public class RuleLoader {
    private static final String SYNCED_BLACKLIST_PATH = "../reliable_remover.json";

    record LoadedRules(Map<Action, List<RemovalRule>> rulesByAction, Set<String> globallyBanned, List<String> blacklist) {
    }

    static LoadedRules load(Collection<List<RemovalRule>> dynamicRules, boolean validate) {
        Map<Action, List<RemovalRule>> rulesByAction = new EnumMap<>(Action.class);
        for (Action action : Action.values()) rulesByAction.put(action, new ArrayList<>());

        List<ConfigSync.SyncedFile> remoteFiles = ConfigSync.getRemoteFiles(Constants.MOD_ID);
        boolean usingRemoteFiles = remoteFiles != null;
        List<String> blacklist = usingRemoteFiles ? parseRemoteFiles(remoteFiles, rulesByAction) : parseLocalFiles(rulesByAction);

        if (validate) {
            validateRules(rulesByAction);
            if (blacklist != null) {
                blacklist = validBlacklistEntries(blacklist);
                if (!usingRemoteFiles) {
                    ModConfig.get().blacklistedItems = blacklist;
                }
            }
        }

        addDynamicRules(dynamicRules, rulesByAction);

        Set<String> globallyBanned = ConcurrentHashMap.newKeySet();
        moveUnconditionalRemovalsToBanList(rulesByAction, globallyBanned);
        return new LoadedRules(rulesByAction, globallyBanned, blacklist);
    }

    private static List<String> parseRemoteFiles(List<ConfigSync.SyncedFile> remoteFiles, Map<Action, List<RemovalRule>> rulesByAction) {
        List<String> blacklist = new ArrayList<>();
        for (ConfigSync.SyncedFile file : remoteFiles) {
            if (file.path().equals(SYNCED_BLACKLIST_PATH)) {
                blacklist.addAll(parseSyncedBlacklist(file.content()));
            } else {
                RuleParser.parseString(file.path(), file.content(), rulesByAction);
            }
        }
        return blacklist;
    }

    private static List<String> parseLocalFiles(Map<Action, List<RemovalRule>> rulesByAction) {
        Path configDir = configDir();

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
                files.forEach(path -> RuleParser.parseFile(path, rulesByAction));
            }
        } catch (Exception e) {
            Constants.LOG.error("Failed to load removal rules", e);
        }

        if (!hasFiles) generateDefaultConfig(configDir);
        return ModConfig.get().blacklistedItems;
    }

    private static void addDynamicRules(Collection<List<RemovalRule>> dynamicRules, Map<Action, List<RemovalRule>> rulesByAction) {
        for (List<RemovalRule> rules : dynamicRules) {
            for (RemovalRule rule : rules) {
                if (rule.actions != null && !rule.actions.isEmpty()) {
                    for (Action action : rule.actions) {
                        rulesByAction.computeIfAbsent(action, a -> new ArrayList<>()).add(rule);
                    }
                } else {
                    Action action = rule.action != null ? rule.action : Action.REMOVE;
                    rulesByAction.computeIfAbsent(action, a -> new ArrayList<>()).add(rule);
                }
            }
        }
    }

    static void registerRecipeReplacements(Map<Action, List<RemovalRule>> rulesByAction) {
        ReliableRecipesAPI.clearItemReplacements();
        List<RemovalRule> replacementRules = new ArrayList<>();
        for (List<RemovalRule> rules : rulesByAction.values()) {
            for (RemovalRule rule : rules) {
                if (!rule.hasReplacement()) continue;
                replacementRules.add(rule);
                String replacement = rule.getReplacementItemId();
                if (rule.items != null) {
                    for (String item : rule.items) ReliableRecipesAPI.registerItemReplacement(item, replacement);
                }
                if (rule.tags != null) {
                    for (String tag : rule.tags) ReliableRecipesAPI.registerItemReplacement(withHash(tag), replacement);
                }
            }
        }

        Map<String, String> registered = ReliableRecipesAPI.getReplacements();
        for (RemovalRule rule : replacementRules) {
            if (rule.not == null) continue;
            List<String> excluded = new ArrayList<>();
            if (rule.not.items != null) excluded.addAll(rule.not.items);
            if (rule.not.tags != null) {
                for (String tag : rule.not.tags) excluded.add(withHash(tag));
            }
            for (String id : excluded) {
                if (!registered.containsKey(id)) ReliableRecipesAPI.registerItemReplacement(id, id);
            }
        }
    }

    private static String withHash(String tag) {
        return tag.startsWith("#") ? tag : "#" + tag;
    }

    private static Path configDir() {
        return Services.PLATFORM.getConfigDirectory().resolve("reliable_remover");
    }

    public static List<ConfigSync.SyncedFile> createSyncSnapshot() {
        List<ConfigSync.SyncedFile> files = new ArrayList<>(ConfigSync.readDirectory(configDir()));

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

    private static List<String> validBlacklistEntries(List<String> blacklist) {
        List<String> valid = new ArrayList<>(blacklist);
        valid.removeIf(itemId -> {
            if (itemId.startsWith("#")) return false;
            Identifier id = Identifier.tryParse(itemId);
            if (id == null) {
                Constants.LOG.warn("Reliable Remover: Skipping invalid blacklisted item ID '{}'.", itemId);
                return true;
            }
            if (!isRegisteredItemBlockFluidOrEffect(id)) {
                Constants.LOG.warn("Reliable Remover: Skipping invalid blacklisted item/block/fluid/effect ID '{}'.", itemId);
                return true;
            }
            return false;
        });
        return valid;
    }

    private static void validateRules(Map<Action, List<RemovalRule>> rulesByAction) {
        for (List<RemovalRule> rules : rulesByAction.values()) {
            for (RemovalRule rule : rules) {
                if (rule.items != null) {
                    rule.items.removeIf(itemId -> isInvalidRuleItem(rule, itemId));
                }
                if (rule.enchantments != null) {
                    rule.enchantments.removeIf(enchId -> !enchId.startsWith("#") && isUnparseable(enchId, enchId, "enchantment"));
                }
                if (rule.tags != null) {
                    rule.tags.removeIf(tagId -> isUnparseable(tagId.startsWith("#") ? tagId.substring(1) : tagId, tagId, "tag"));
                }
            }
        }
    }

    private static boolean isInvalidRuleItem(RemovalRule rule, String itemId) {
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
        } else if (rule.action != Action.REMOVE_ENCHANTMENT && rule.action != Action.REMOVE_PAINTING) {
            if (!isRegisteredItemBlockFluidOrEffect(id)) {
                Constants.LOG.warn("Reliable Remover: Skipping invalid item/block/fluid/effect ID '{}'.", itemId);
                return true;
            }
        }
        return false;
    }

    private static boolean isUnparseable(String id, String original, String kind) {
        if (Identifier.tryParse(id) != null) return false;
        Constants.LOG.warn("Reliable Remover: Skipping invalid {} ID '{}'.", kind, original);
        return true;
    }

    private static boolean isRegisteredItemBlockFluidOrEffect(Identifier id) {
        return BuiltInRegistries.ITEM.containsKey(id)
                || BuiltInRegistries.BLOCK.containsKey(id)
                || BuiltInRegistries.FLUID.containsKey(id)
                || BuiltInRegistries.MOB_EFFECT.containsKey(id);
    }

    private static void moveUnconditionalRemovalsToBanList(Map<Action, List<RemovalRule>> rulesByAction, Set<String> globallyBanned) {
        List<RemovalRule> removeRules = rulesByAction.get(Action.REMOVE);
        if (removeRules == null) return;
        Iterator<RemovalRule> iterator = removeRules.iterator();
        while (iterator.hasNext()) {
            RemovalRule rule = iterator.next();
            if (isUnconditionalItemRemoval(rule)) {
                globallyBanned.addAll(rule.items);
                iterator.remove();
            }
        }
    }

    private static boolean isUnconditionalItemRemoval(RemovalRule rule) {
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
}
