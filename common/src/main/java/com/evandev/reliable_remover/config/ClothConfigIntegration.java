package com.evandev.reliable_remover.config;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

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

        general.addEntry(entryBuilder.startBooleanToggle(Component.translatable("config.reliable_remover.option.remove_creative_tab_items"), config.removeItemsFromCreativeTabs)
                .setDefaultValue(true)
                .setTooltip(Component.translatable("config.reliable_remover.option.remove_creative_tab_items.tooltip"))
                .setSaveConsumer(newValue -> config.removeItemsFromCreativeTabs = newValue)
                .build());

        general.addEntry(entryBuilder.startBooleanToggle(Component.translatable("config.reliable_remover.option.remove_emi_items"), config.removeItemsFromEmi)
                .setDefaultValue(true)
                .setTooltip(Component.translatable("config.reliable_remover.option.remove_emi_items.tooltip"))
                .setSaveConsumer(newValue -> config.removeItemsFromEmi = newValue)
                .build());

        general.addEntry(entryBuilder.startBooleanToggle(Component.translatable("config.reliable_remover.option.remove_inventory_items"), config.removeItemsFromInventories)
                .setDefaultValue(true)
                .setTooltip(Component.translatable("config.reliable_remover.option.remove_inventory_items.tooltip"))
                .setSaveConsumer(newValue -> config.removeItemsFromInventories = newValue)
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

        return builder.build();
    }
}