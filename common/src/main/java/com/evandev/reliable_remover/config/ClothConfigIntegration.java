package com.evandev.reliable_remover.config;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;

public class ClothConfigIntegration {

    public static Screen createScreen(Screen parent) {
        ModConfig.load();
        ModConfig config = ModConfig.get();

        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Component.translatable("config.reliable_remover.title"));

        builder.setSavingRunnable(() -> {
            ModConfig.save();
            RuleManager.load();
        });

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

        general.addEntry(entryBuilder.startBooleanToggle(Component.translatable("config.reliable_remover.option.remove_cnm_children"), config.removeCnmChildren)
                .setDefaultValue(true)
                .setTooltip(Component.translatable("config.reliable_remover.option.remove_cnm_children.tooltip"))
                .setSaveConsumer(newValue -> config.removeCnmChildren = newValue)
                .build());

        ConfigCategory blacklistCategory = builder.getOrCreateCategory(Component.translatable("config.reliable_remover.category.blacklist"));

        blacklistCategory.addEntry(entryBuilder.startStrList(Component.translatable("config.reliable_remover.option.blacklisted_items"), config.blacklistedItems)
                .setDefaultValue(new ArrayList<>())
                .setTooltip(Component.translatable("config.reliable_remover.option.blacklisted_items.tooltip"))
                .setSaveConsumer(newValue -> config.blacklistedItems = newValue)
                .build());

        ConfigCategory emiCategory = builder.getOrCreateCategory(Component.translatable("config.reliable_remover.category.emi"));

        emiCategory.addEntry(entryBuilder.startBooleanToggle(Component.translatable("config.reliable_remover.option.enable_emi_removal"), config.enableEmiRemoval)
                .setDefaultValue(false)
                .setTooltip(Component.translatable("config.reliable_remover.option.enable_emi_removal.tooltip"))
                .setSaveConsumer(newValue -> config.enableEmiRemoval = newValue)
                .build());

        emiCategory.addEntry(entryBuilder.startBooleanToggle(Component.translatable("config.reliable_remover.option.show_toast"), config.showEmiToast)
                .setDefaultValue(true)
                .setTooltip(Component.translatable("config.reliable_remover.option.show_toast.tooltip"))
                .setSaveConsumer(newValue -> config.showEmiToast = newValue)
                .build());

        emiCategory.addEntry(entryBuilder.startBooleanToggle(Component.translatable("config.reliable_remover.option.show_chat_messages"), config.showEmiChatMessages)
                .setDefaultValue(true)
                .setTooltip(Component.translatable("config.reliable_remover.option.show_chat_messages.tooltip"))
                .setSaveConsumer(newValue -> config.showEmiChatMessages = newValue)
                .build());

        emiCategory.addEntry(entryBuilder.startBooleanToggle(Component.translatable("config.reliable_remover.option.reload_emi"), config.reloadAfterRemoval)
                .setDefaultValue(false)
                .setTooltip(Component.translatable("config.reliable_remover.option.reload_emi.tooltip"))
                .setSaveConsumer(newValue -> config.reloadAfterRemoval = newValue)
                .build());

        return builder.build();
    }
}