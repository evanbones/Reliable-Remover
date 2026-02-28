package com.evandev.reliable_remover.config;

import com.evandev.reliable_remover.Constants;
import com.evandev.reliable_remover.platform.Services;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ModConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File CONFIG_FILE = Services.PLATFORM.getConfigDirectory().resolve("reliable_remover.json").toFile();

    private static ModConfig INSTANCE;

    public boolean showAttackMessage = true;
    public boolean showHandSwingMessage = true;
    public boolean showRemovalMessage = true;
    public boolean removeItemsFromCreativeTabs = true;
    public boolean removeItemsFromEmi = true;
    public boolean removeItemsFromInventories = true;
    public boolean removeItemsOnInventoryOpen = true;
    public boolean removeDroppedItems = true;
    public boolean removeItemsFromLootChests = true;
    public boolean removeItemsFromStorage = true;
    public boolean removeItemsFromInfoTabs = true;
    public boolean removeMobEquipment = true;

    public boolean enableEmiRemoval = false;
    public boolean showEmiToast = true;
    public boolean showEmiChatMessages = true;
    public boolean reloadAfterRemoval = false;
    public List<String> blacklistedItems = new ArrayList<>();

    public static ModConfig get() {
        if (INSTANCE == null) {
            load();
        }
        return INSTANCE;
    }

    public static void load() {
        if (CONFIG_FILE.exists()) {
            try (FileReader reader = new FileReader(CONFIG_FILE)) {
                INSTANCE = GSON.fromJson(reader, ModConfig.class);
            } catch (Exception e) {
                Constants.LOG.error("Failed to load reliable_remover.json", e);
                INSTANCE = new ModConfig();
            }
        } else {
            INSTANCE = new ModConfig();
            save();
        }
    }

    public static void save() {
        try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
            GSON.toJson(INSTANCE, writer);
        } catch (IOException e) {
            Constants.LOG.error("Failed to save reliable_remover.json", e);
        }
    }
}