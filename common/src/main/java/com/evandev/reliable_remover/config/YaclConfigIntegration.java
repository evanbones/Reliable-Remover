package com.evandev.reliable_remover.config;

import dev.isxander.yacl3.api.*;
import dev.isxander.yacl3.api.controller.StringControllerBuilder;
import dev.isxander.yacl3.api.controller.TickBoxControllerBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class YaclConfigIntegration {

    public static Screen createScreen(Screen parent) {
        ModConfig config = ModConfig.get();

        YetAnotherConfigLib.Builder builder = YetAnotherConfigLib.createBuilder()
                .title(Component.translatable("config.reliable_remover.title"))
                .save(ModConfig::save);

        ConfigCategory.Builder generalCategory = ConfigCategory.createBuilder()
                .name(Component.translatable("config.reliable_remover.category.general"));

        generalCategory.option(createBoolOption("show_removal_message", true, () -> config.showRemovalMessage, val -> config.showRemovalMessage = val));
        generalCategory.option(createBoolOption("show_attack_message", true, () -> config.showAttackMessage, val -> config.showAttackMessage = val));
        generalCategory.option(createBoolOption("show_hand_swing_message", true, () -> config.showHandSwingMessage, val -> config.showHandSwingMessage = val));
        generalCategory.option(createBoolOption("remove_creative_tab_items", true, () -> config.removeItemsFromCreativeTabs, val -> config.removeItemsFromCreativeTabs = val));
        generalCategory.option(createBoolOption("remove_rrv_items", true, () -> config.removeItemsFromRrv, val -> config.removeItemsFromRrv = val));
        generalCategory.option(createBoolOption("remove_inventory_items", true, () -> config.removeItemsFromInventories, val -> config.removeItemsFromInventories = val));
        generalCategory.option(createBoolOption("remove_on_inventory_open", true, () -> config.removeItemsOnInventoryOpen, val -> config.removeItemsOnInventoryOpen = val));
        generalCategory.option(createBoolOption("remove_dropped_items", true, () -> config.removeDroppedItems, val -> config.removeDroppedItems = val));
        generalCategory.option(createBoolOption("remove_loot_items", true, () -> config.removeItemsFromLootChests, val -> config.removeItemsFromLootChests = val));
        generalCategory.option(createBoolOption("remove_storage_items", true, () -> config.removeItemsFromStorage, val -> config.removeItemsFromStorage = val));
        generalCategory.option(createBoolOption("remove_trades", true, () -> config.removeItemsFromTrades, val -> config.removeItemsFromTrades = val));
        generalCategory.option(createBoolOption("remove_recipes", true, () -> config.removeRecipes, val -> config.removeRecipes = val));
        generalCategory.option(createBoolOption("remove_info_tabs", true, () -> config.removeItemsFromInfoTabs, val -> config.removeItemsFromInfoTabs = val));
        generalCategory.option(createBoolOption("remove_mob_equipment", true, () -> config.removeMobEquipment, val -> config.removeMobEquipment = val));

        ConfigCategory.Builder blacklistCategory = ConfigCategory.createBuilder()
                .name(Component.translatable("config.reliable_remover.category.blacklist"));

        blacklistCategory.group(ListOption.<String>createBuilder(String.class)
                .name(Component.translatable("config.reliable_remover.option.blacklisted_items"))
                .description(OptionDescription.of(Component.translatable("config.reliable_remover.option.blacklisted_items.tooltip")))
                .binding(
                        new ArrayList<>(),
                        () -> config.blacklistedItems,
                        newValue -> {
                            config.blacklistedItems = newValue;
                            RuleManager.load();
                        }
                )
                .controller(StringControllerBuilder::create)
                .initial("")
                .build());

        ConfigCategory.Builder rrvCategory = ConfigCategory.createBuilder()
                .name(Component.translatable("config.reliable_remover.category.rrv"));

        rrvCategory.option(createBoolOption("enable_rrv_removal", false, () -> config.enableRrvRemoval, val -> config.enableRrvRemoval = val));
        rrvCategory.option(createBoolOption("show_toast", true, () -> config.showRrvToast, val -> config.showRrvToast = val));
        rrvCategory.option(createBoolOption("show_chat_messages", true, () -> config.showRrvChatMessages, val -> config.showRrvChatMessages = val));
        rrvCategory.option(createBoolOption("reload_rrv", false, () -> config.reloadAfterRemoval, val -> config.reloadAfterRemoval = val));

        return builder
                .category(generalCategory.build())
                .category(blacklistCategory.build())
                .category(rrvCategory.build())
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
