package com.evandev.reliable_remover.config;

import com.evandev.reliable_remover.Constants;
import com.evandev.reliable_remover.platform.Services;
import com.google.gson.*;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.file.Path;

public class RuleConfigIO {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_DIR = Services.PLATFORM.getConfigDirectory().resolve("reliable_remover");

    public static boolean addRemovalRule(String itemId) {
        File file = CONFIG_DIR.resolve("generated_removals.json").toFile();
        JsonArray root = new JsonArray();

        if (file.exists()) {
            try (FileReader reader = new FileReader(file)) {
                root = JsonParser.parseReader(reader).getAsJsonArray();
            } catch (Exception e) {
                Constants.LOG.error("Failed to read generated config", e);
            }
        }

        JsonObject bulkRemoveRule = null;
        for (JsonElement e : root) {
            if (e.isJsonObject()) {
                JsonObject obj = e.getAsJsonObject();
                if ("remove".equalsIgnoreCase(obj.get("action").getAsString()) && obj.has("items")) {
                    bulkRemoveRule = obj;
                    break;
                }
            }
        }

        if (bulkRemoveRule == null) {
            bulkRemoveRule = new JsonObject();
            bulkRemoveRule.addProperty("action", "remove");
            bulkRemoveRule.add("items", new JsonArray());
            root.add(bulkRemoveRule);
        }

        JsonArray items = bulkRemoveRule.getAsJsonArray("items");
        for (JsonElement e : items) {
            if (e.getAsString().equals(itemId)) return false;
        }

        items.add(itemId);
        try (FileWriter writer = new FileWriter(file)) {
            GSON.toJson(root, writer);
            RuleManager.load();
            return true;
        } catch (Exception e) {
            Constants.LOG.error("Failed to save generated config", e);
        }
        return false;
    }

    public static boolean removeRemovalRule(String itemId) {
        File file = CONFIG_DIR.resolve("generated_removals.json").toFile();
        if (!file.exists()) return false;

        try (FileReader reader = new FileReader(file)) {
            JsonArray root = JsonParser.parseReader(reader).getAsJsonArray();
            boolean changed = false;

            for (int i = 0; i < root.size(); i++) {
                JsonElement e = root.get(i);
                if (!e.isJsonObject()) continue;

                JsonObject rule = e.getAsJsonObject();
                if ("remove".equalsIgnoreCase(rule.get("action").getAsString()) && rule.has("items")) {
                    JsonArray items = rule.getAsJsonArray("items");
                    for (int j = 0; j < items.size(); j++) {
                        if (items.get(j).getAsString().equals(itemId)) {
                            items.remove(j);
                            changed = true;
                            break;
                        }
                    }
                    if (items.isEmpty()) {
                        root.remove(i);
                        i--;
                    }
                }
            }

            if (changed) {
                try (FileWriter writer = new FileWriter(file)) {
                    GSON.toJson(root, writer);
                    RuleManager.load();
                    return true;
                }
            }
        } catch (Exception e) {
            Constants.LOG.error("Failed to update generated config", e);
        }
        return false;
    }
}