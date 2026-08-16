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
                        if (e.isJsonObject()) normalizeRule(e.getAsJsonObject());
                        addRule(GSON.fromJson(e, RemovalRule.class), rulesByAction);
                    }
                } else if (json.isJsonObject()) {
                    normalizeRule(json.getAsJsonObject());
                    addRule(GSON.fromJson(json, RemovalRule.class), rulesByAction);
                }
            }
        } catch (Exception e) {
            Constants.LOG.error("Error parsing file: {}", path, e);
        }
    }

    private static void normalizeRule(JsonObject obj) {
        if (obj.has("action")) {
            JsonElement act = obj.get("action");
            if (act.isJsonArray()) {
                JsonArray actionArray = act.getAsJsonArray();
                JsonArray actions = obj.has("actions") && obj.get("actions").isJsonArray()
                        ? obj.getAsJsonArray("actions")
                        : new JsonArray();
                actionArray.forEach(actions::add);
                obj.remove("action");
                obj.add("actions", actions);
            }
        }

        if (obj.has("actions")) {
            JsonElement acts = obj.get("actions");
            if (acts.isJsonPrimitive()) {
                JsonArray arr = new JsonArray();
                arr.add(acts);
                obj.add("actions", arr);
            }
        }

        if (obj.has("pattern") && obj.get("pattern").isJsonArray()) {
            JsonArray patternArray = obj.getAsJsonArray("pattern");
            JsonArray patterns = obj.has("patterns") && obj.get("patterns").isJsonArray()
                    ? obj.getAsJsonArray("patterns")
                    : new JsonArray();
            patternArray.forEach(patterns::add);
            obj.remove("pattern");
            obj.add("patterns", patterns);
        }

        if (obj.has("not") && obj.get("not").isJsonObject()) {
            normalizeRule(obj.getAsJsonObject("not"));
        }
    }

    private static void addRule(RemovalRule rule, Map<Action, List<RemovalRule>> rulesByAction) {
        Set<Action> targetActions = new LinkedHashSet<>();
        if (rule.actions != null && !rule.actions.isEmpty()) {
            targetActions.addAll(rule.actions);
        }
        if (rule.action != null) {
            targetActions.add(rule.action);
        }
        if (targetActions.isEmpty()) {
            targetActions.add(Action.REMOVE);
        }

        for (Action act : targetActions) {
            RemovalRule ruleForAction = copyRuleForAction(rule, act);
            rulesByAction.computeIfAbsent(act, k -> new ArrayList<>()).add(ruleForAction);
        }
    }

    private static RemovalRule copyRuleForAction(RemovalRule original, Action act) {
        RemovalRule copy = new RemovalRule();
        copy.action = act;
        copy.actions = original.actions != null ? new HashSet<>(original.actions) : new HashSet<>();
        copy.items = original.items != null ? new HashSet<>(original.items) : new HashSet<>();
        copy.dimensions = original.dimensions != null ? new HashSet<>(original.dimensions) : new HashSet<>();
        copy.entities = original.entities != null ? new HashSet<>(original.entities) : new HashSet<>();
        copy.mod = original.mod != null ? new HashSet<>(original.mod) : new HashSet<>();
        copy.tags = original.tags != null ? new HashSet<>(original.tags) : new HashSet<>();
        copy.registry = original.registry != null ? new HashSet<>(original.registry) : new HashSet<>();
        copy.tagType = original.tagType != null ? new HashSet<>(original.tagType) : new HashSet<>();
        copy.pattern = original.pattern;
        copy.patterns = original.patterns != null ? new ArrayList<>(original.patterns) : new ArrayList<>();
        copy.nbt = original.nbt != null ? new ArrayList<>(original.nbt) : new ArrayList<>();
        copy.replaceWith = original.replaceWith;
        copy.not = original.not;
        return copy;
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