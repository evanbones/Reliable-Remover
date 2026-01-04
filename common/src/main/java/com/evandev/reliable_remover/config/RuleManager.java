package com.evandev.reliable_remover.config;

import com.evandev.reliable_remover.Constants;
import com.evandev.reliable_remover.data.RemovalRule;
import com.evandev.reliable_remover.platform.Services;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.io.FileReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class RuleManager {
    private static final Gson GSON = new Gson();
    private static final List<RemovalRule> RULES = new ArrayList<>();

    private static final List<String> REMOVE_CACHE = new ArrayList<>();
    private static final List<String> ATTACK_CACHE = new ArrayList<>();

    public static void load() {
        RULES.clear();
        clearCaches();

        Path configDir = Services.PLATFORM.getConfigDirectory().resolve("reliable_remover");
        if (!Files.exists(configDir)) {
            try { Files.createDirectories(configDir); } catch (Exception ignored) {}
            return;
        }

        try (Stream<Path> paths = Files.walk(configDir)) {
            paths.filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".json"))
                    .forEach(RuleManager::parseFile);
        } catch (Exception e) {
            Constants.LOG.error("Failed to load removal rules", e);
        }

        Constants.LOG.info("Loaded {} reliable remover rules.", RULES.size());
    }

    private static void parseFile(Path path) {
        try (FileReader reader = new FileReader(path.toFile())) {
            JsonElement json = JsonParser.parseReader(reader);
            if (json.isJsonArray()) {
                for (JsonElement e : json.getAsJsonArray()) {
                    RULES.add(GSON.fromJson(e, RemovalRule.class));
                }
            } else if (json.isJsonObject()) {
                RULES.add(GSON.fromJson(json, RemovalRule.class));
            }
        } catch (Exception e) {
            Constants.LOG.error("Error parsing file: " + path, e);
        }
    }

    private static void clearCaches() {
        REMOVE_CACHE.clear();
        ATTACK_CACHE.clear();
    }

    public static boolean isHidden(String itemId) {
        return checkRules(itemId, RemovalRule.Action.REMOVE);
    }

    public static boolean isAttackBlocked(String itemId) {
        return checkRules(itemId, RemovalRule.Action.REMOVE_ATTACKS) || isHidden(itemId);
    }

    private static boolean checkRules(String itemId, RemovalRule.Action action) {
        for (RemovalRule rule : RULES) {
            if (rule.action == action && rule.filter.matches(itemId)) {
                return true;
            }
        }
        return false;
    }
}