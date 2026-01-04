package com.evandev.reliable_remover.config;

import com.evandev.reliable_remover.Constants;
import com.evandev.reliable_remover.data.RemovalRule;
import com.evandev.reliable_remover.platform.Services;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;

import java.io.FileReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class RuleManager {
    private static final Gson GSON = new GsonBuilder().setLenient().create();
    private static final List<RemovalRule> RULES = new ArrayList<>();

    public static void load() {
        RULES.clear();
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
                for (JsonElement e : json.getAsJsonArray()) RULES.add(GSON.fromJson(e, RemovalRule.class));
            } else if (json.isJsonObject()) {
                RULES.add(GSON.fromJson(json, RemovalRule.class));
            }
        } catch (Exception e) {
            Constants.LOG.error("Error parsing file: {}", path, e);
        }
    }

    public static boolean isHidden(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        return checkRules(stack, id, RemovalRule.Action.REMOVE);
    }

    public static boolean isHidden(String itemId) {
        return checkRules(null, itemId, RemovalRule.Action.REMOVE);
    }

    public static boolean isAttackBlocked(ItemStack stack) {
        String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        return checkRules(stack, id, RemovalRule.Action.REMOVE_ATTACKS) || isHidden(stack);
    }

    public static boolean isInteractionBlocked(ItemStack stack) {
        String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        return checkRules(stack, id, RemovalRule.Action.REMOVE_INTERACTIONS) || isHidden(stack);
    }

    private static boolean checkRules(ItemStack stack, String itemId, RemovalRule.Action action) {
        for (RemovalRule rule : RULES) {
            if (rule.action == action) {
                if (rule.filter != null) {
                    if (stack != null) {
                        if (rule.filter.matches(stack, itemId)) return true;
                    } else {
                        if (rule.filter.matches(itemId)) return true;
                    }
                }
            }
        }
        return false;
    }
}