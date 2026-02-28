package com.evandev.reliable_remover.config;

import com.evandev.reliable_remover.Constants;
import com.evandev.reliable_remover.data.Action;
import com.evandev.reliable_remover.data.RemovalRule;
import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;

import java.io.FileReader;
import java.lang.reflect.Type;
import java.nio.file.Path;
import java.util.*;

public class RuleParser {
    public static final Gson GSON = new GsonBuilder()
            .setLenient()
            .registerTypeAdapter(new TypeToken<Set<String>>() {
            }.getType(), new StringOrSetDeserializer())
            .registerTypeAdapter(new TypeToken<List<String>>() {
            }.getType(), new StringOrListDeserializer())
            .create();

    public static void parseFile(Path path, Map<Action, List<RemovalRule>> rulesByAction) {
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

    private static class StringOrSetDeserializer implements JsonDeserializer<Set<String>> {
        @Override
        public Set<String> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            Set<String> set = new HashSet<>();
            if (json.isJsonArray()) {
                json.getAsJsonArray().forEach(e -> set.add(e.getAsString()));
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
                json.getAsJsonArray().forEach(e -> list.add(e.getAsString()));
            } else if (json.isJsonPrimitive()) {
                list.add(json.getAsString());
            }
            return list;
        }
    }
}