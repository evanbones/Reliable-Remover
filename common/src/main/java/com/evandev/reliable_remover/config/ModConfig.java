package com.evandev.reliable_remover.config;

import com.evandev.reliable_remover.Constants;
import com.evandev.reliable_remover.platform.Services;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.isxander.yacl3.api.*;
import dev.isxander.yacl3.api.controller.StringControllerBuilder;
import dev.isxander.yacl3.api.controller.TickBoxControllerBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

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
    public boolean removeItemsFromTrades = true;
    public boolean removeMobEquipment = true;
    public boolean removeCnmChildren = true;

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
                save();
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

    public static Screen createScreen(Screen parent) {
        load();

        YetAnotherConfigLib.Builder builder = YetAnotherConfigLib.createBuilder()
                .title(Component.translatable("config.reliable_remover.title"))
                .save(() -> {
                    save();
                    RuleManager.load();
                });

        ConfigCategory.Builder general = ConfigCategory.createBuilder()
                .name(Component.translatable("config.reliable_remover.category.general"))
                .option(createBoolOption("show_removal_message", true, () -> get().showRemovalMessage, val -> get().showRemovalMessage = val))
                .option(createBoolOption("show_attack_message", true, () -> get().showAttackMessage, val -> get().showAttackMessage = val))
                .option(createBoolOption("show_hand_swing_message", true, () -> get().showHandSwingMessage, val -> get().showHandSwingMessage = val))
                .option(createBoolOption("remove_creative_tab_items", true, () -> get().removeItemsFromCreativeTabs, val -> get().removeItemsFromCreativeTabs = val))
                .option(createBoolOption("remove_emi_items", true, () -> get().removeItemsFromEmi, val -> get().removeItemsFromEmi = val))
                .option(createBoolOption("remove_inventory_items", true, () -> get().removeItemsFromInventories, val -> get().removeItemsFromInventories = val))
                .option(createBoolOption("remove_on_inventory_open", true, () -> get().removeItemsOnInventoryOpen, val -> get().removeItemsOnInventoryOpen = val))
                .option(createBoolOption("remove_dropped_items", true, () -> get().removeDroppedItems, val -> get().removeDroppedItems = val))
                .option(createBoolOption("remove_loot_items", true, () -> get().removeItemsFromLootChests, val -> get().removeItemsFromLootChests = val))
                .option(createBoolOption("remove_storage_items", true, () -> get().removeItemsFromStorage, val -> get().removeItemsFromStorage = val))
                .option(createBoolOption("remove_trades", true, () -> get().removeItemsFromTrades, val -> get().removeItemsFromTrades = val))
                .option(createBoolOption("remove_info_tabs", true, () -> get().removeItemsFromInfoTabs, val -> get().removeItemsFromInfoTabs = val))
                .option(createBoolOption("remove_mob_equipment", true, () -> get().removeMobEquipment, val -> get().removeMobEquipment = val))
                .option(createBoolOption("remove_cnm_children", true, () -> get().removeCnmChildren, val -> get().removeCnmChildren = val));

        ConfigCategory.Builder blacklist = ConfigCategory.createBuilder()
                .name(Component.translatable("config.reliable_remover.category.blacklist"))
                .group(ListOption.<String>createBuilder()
                        .name(Component.translatable("config.reliable_remover.option.blacklisted_items"))
                        .description(OptionDescription.of(Component.translatable("config.reliable_remover.option.blacklisted_items.tooltip")))
                        .binding(new ArrayList<>(), () -> get().blacklistedItems, val -> get().blacklistedItems = val)
                        .controller(StringControllerBuilder::create)
                        .initial("")
                        .build());

        ConfigCategory.Builder emi = ConfigCategory.createBuilder()
                .name(Component.translatable("config.reliable_remover.category.emi"))
                .option(createBoolOption("enable_emi_removal", false, () -> get().enableEmiRemoval, val -> get().enableEmiRemoval = val))
                .option(createBoolOption("show_toast", true, () -> get().showEmiToast, val -> get().showEmiToast = val))
                .option(createBoolOption("show_chat_messages", true, () -> get().showEmiChatMessages, val -> get().showEmiChatMessages = val))
                .option(createBoolOption("reload_emi", false, () -> get().reloadAfterRemoval, val -> get().reloadAfterRemoval = val));

        return builder
                .category(general.build())
                .category(blacklist.build())
                .category(emi.build())
                .build()
                .generateScreen(parent);
    }

    private static Option<Boolean> createBoolOption(String name, boolean defaultValue, Supplier<Boolean> getter, Consumer<Boolean> setter) {
        return Option.<Boolean>createBuilder()
                .name(Component.translatable("config.reliable_remover.option." + name))
                .description(OptionDescription.of(Component.translatable("config.reliable_remover.option." + name + ".tooltip")))
                .binding(defaultValue, getter, setter)
                .controller(TickBoxControllerBuilder::create)
                .build();
    }
}