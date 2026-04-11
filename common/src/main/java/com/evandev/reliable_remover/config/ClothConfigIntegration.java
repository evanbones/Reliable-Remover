package com.evandev.reliable_remover.config;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;

public class ClothConfigIntegration {

    public static Screen createScreen(Screen parent) {
        ModConfig config = ModConfig.get();

        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Component.translatable("config.reliable_remover.title"));

        builder.setSavingRunnable(ModConfig::save);

        ConfigCategory general = builder.getOrCreateCategory(Component.translatable("config.reliable_remover.category.general"));
        ConfigEntryBuilder entryBuilder = builder.entryBuilder();

        general.addEntry(entryBuilder.startBooleanToggle(Component.translatable("config.reliable_remover.option.show_removal_message"), config.showRemovalMessage)
                .setDefaultValue(true)
                .setTooltip(Component.translatable("config.reliable_remover.option.show_removal_message.tooltip"))
                .setSaveConsumer(newValue -> config.showRemovalMessage = newValue)
                .build());

        general.addEntry(entryBuilder.startBooleanToggle(Component.translatable("config.reliable_remover.option.show_attack_message"), config.showAttackMessage)
                .setDefaultValue(true)
                .setTooltip(Component.translatable("config.reliable_remover.option.show_attack_message.tooltip"))
                .setSaveConsumer(newValue -> config.showAttackMessage = newValue)
                .build());

        general.addEntry(entryBuilder.startBooleanToggle(Component.translatable("config.reliable_remover.option.show_hand_swing_message"), config.showHandSwingMessage)
                .setDefaultValue(true)
                .setTooltip(Component.translatable("config.reliable_remover.option.show_hand_swing_message.tooltip"))
                .setSaveConsumer(newValue -> config.showHandSwingMessage = newValue)
                .build());

        general.addEntry(entryBuilder.startBooleanToggle(Component.translatable("config.reliable_remover.option.remove_creative_tab_items"), config.removeItemsFromCreativeTabs)
                .setDefaultValue(true)
                .setTooltip(Component.translatable("config.reliable_remover.option.remove_creative_tab_items.tooltip"))
                .setSaveConsumer(newValue -> config.removeItemsFromCreativeTabs = newValue)
                .build());

        general.addEntry(entryBuilder.startBooleanToggle(Component.translatable("config.reliable_remover.option.remove_rrv_items"), config.removeItemsFromRrv)
                .setDefaultValue(true)
                .setTooltip(Component.translatable("config.reliable_remover.option.remove_rrv_items.tooltip"))
                .setSaveConsumer(newValue -> config.removeItemsFromRrv = newValue)
                .build());

        general.addEntry(entryBuilder.startBooleanToggle(Component.translatable("config.reliable_remover.option.remove_inventory_items"), config.removeItemsFromInventories)
                .setDefaultValue(true)
                .setTooltip(Component.translatable("config.reliable_remover.option.remove_inventory_items.tooltip"))
                .setSaveConsumer(newValue -> config.removeItemsFromInventories = newValue)
                .build());

        general.addEntry(entryBuilder.startBooleanToggle(Component.translatable("config.reliable_remover.option.remove_on_inventory_open"), config.removeItemsOnInventoryOpen)
                .setDefaultValue(true)
                .setTooltip(Component.translatable("config.reliable_remover.option.remove_on_inventory_open.tooltip"))
                .setSaveConsumer(newValue -> config.removeItemsOnInventoryOpen = newValue)
                .build());

        general.addEntry(entryBuilder.startBooleanToggle(Component.translatable("config.reliable_remover.option.remove_dropped_items"), config.removeDroppedItems)
                .setDefaultValue(true)
                .setTooltip(Component.translatable("config.reliable_remover.option.remove_dropped_items.tooltip"))
                .setSaveConsumer(newValue -> config.removeDroppedItems = newValue)
                .build());

        general.addEntry(entryBuilder.startBooleanToggle(Component.translatable("config.reliable_remover.option.remove_loot_items"), config.removeItemsFromLootChests)
                .setDefaultValue(true)
                .setTooltip(Component.translatable("config.reliable_remover.option.remove_loot_items.tooltip"))
                .setSaveConsumer(newValue -> config.removeItemsFromLootChests = newValue)
                .build());

        general.addEntry(entryBuilder.startBooleanToggle(Component.translatable("config.reliable_remover.option.remove_storage_items"), config.removeItemsFromStorage)
                .setDefaultValue(true)
                .setTooltip(Component.translatable("config.reliable_remover.option.remove_storage_items.tooltip"))
                .setSaveConsumer(newValue -> config.removeItemsFromStorage = newValue)
                .build());

        general.addEntry(entryBuilder.startBooleanToggle(Component.translatable("config.reliable_remover.option.remove_trades"), config.removeItemsFromTrades)
                .setDefaultValue(true)
                .setTooltip(Component.translatable("config.reliable_remover.option.remove_trades.tooltip"))
                .setSaveConsumer(newValue -> config.removeItemsFromTrades = newValue)
                .build());

        general.addEntry(entryBuilder.startBooleanToggle(Component.translatable("config.reliable_remover.option.remove_recipes"), config.removeRecipes)
                .setDefaultValue(true)
                .setTooltip(Component.translatable("config.reliable_remover.option.remove_recipes.tooltip"))
                .setSaveConsumer(newValue -> config.removeRecipes = newValue)
                .build());

        general.addEntry(entryBuilder.startBooleanToggle(Component.translatable("config.reliable_remover.option.remove_info_tabs"), config.removeItemsFromInfoTabs)
                .setDefaultValue(true)
                .setTooltip(Component.translatable("config.reliable_remover.option.remove_info_tabs.tooltip"))
                .setSaveConsumer(newValue -> config.removeItemsFromInfoTabs = newValue)
                .build());

        general.addEntry(entryBuilder.startBooleanToggle(Component.translatable("config.reliable_remover.option.remove_mob_equipment"), config.removeMobEquipment)
                .setDefaultValue(true)
                .setTooltip(Component.translatable("config.reliable_remover.option.remove_mob_equipment.tooltip"))
                .setSaveConsumer(newValue -> config.removeMobEquipment = newValue)
                .build());

        ConfigCategory blacklistCategory = builder.getOrCreateCategory(Component.translatable("config.reliable_remover.category.blacklist"));

        blacklistCategory.addEntry(entryBuilder.startStrList(Component.translatable("config.reliable_remover.option.blacklisted_items"), config.blacklistedItems)
                .setDefaultValue(new ArrayList<>())
                .setTooltip(Component.translatable("config.reliable_remover.option.blacklisted_items.tooltip"))
                .setSaveConsumer(newValue -> {
                    config.blacklistedItems = newValue;
                    RuleManager.load();
                })
                .build());

        ConfigCategory rrvCategory = builder.getOrCreateCategory(Component.translatable("config.reliable_remover.category.rrv"));

        rrvCategory.addEntry(entryBuilder.startBooleanToggle(Component.translatable("config.reliable_remover.option.enable_rrv_removal"), config.enableRrvRemoval)
                .setDefaultValue(false)
                .setTooltip(Component.translatable("config.reliable_remover.option.enable_rrv_removal.tooltip"))
                .setSaveConsumer(newValue -> config.enableRrvRemoval = newValue)
                .build());

        rrvCategory.addEntry(entryBuilder.startBooleanToggle(Component.translatable("config.reliable_remover.option.show_toast"), config.showRrvToast)
                .setDefaultValue(true)
                .setTooltip(Component.translatable("config.reliable_remover.option.show_toast.tooltip"))
                .setSaveConsumer(newValue -> config.showRrvToast = newValue)
                .build());

        rrvCategory.addEntry(entryBuilder.startBooleanToggle(Component.translatable("config.reliable_remover.option.show_chat_messages"), config.showRrvChatMessages)
                .setDefaultValue(true)
                .setTooltip(Component.translatable("config.reliable_remover.option.show_chat_messages.tooltip"))
                .setSaveConsumer(newValue -> config.showRrvChatMessages = newValue)
                .build());

        rrvCategory.addEntry(entryBuilder.startBooleanToggle(Component.translatable("config.reliable_remover.option.reload_rrv"), config.reloadAfterRemoval)
                .setDefaultValue(false)
                .setTooltip(Component.translatable("config.reliable_remover.option.reload_rrv.tooltip"))
                .setSaveConsumer(newValue -> config.reloadAfterRemoval = newValue)
                .build());

        return builder.build();
    }
}