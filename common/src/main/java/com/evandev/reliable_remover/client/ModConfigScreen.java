package com.evandev.reliable_remover.client;

import com.evandev.reliable_remover.config.ModConfig;
import com.evandev.reliable_remover.config.RuleManager;
import dev.isxander.yacl3.api.*;
import dev.isxander.yacl3.api.controller.StringControllerBuilder;
import dev.isxander.yacl3.api.controller.TickBoxControllerBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class ModConfigScreen {
    public static Screen createScreen(Screen parent) {
        ModConfig.load();

        YetAnotherConfigLib.Builder builder = YetAnotherConfigLib.createBuilder()
                .title(Component.translatable("config.reliable_remover.title"))
                .save(() -> {
                    ModConfig.save();
                    RuleManager.load();
                });

        ConfigCategory.Builder general = ConfigCategory.createBuilder()
                .name(Component.translatable("config.reliable_remover.category.general"))
                .option(createBoolOption("show_removal_message", true, () -> ModConfig.get().showRemovalMessage, val -> ModConfig.get().showRemovalMessage = val))
                .option(createBoolOption("show_attack_message", true, () -> ModConfig.get().showAttackMessage, val -> ModConfig.get().showAttackMessage = val))
                .option(createBoolOption("show_hand_swing_message", true, () -> ModConfig.get().showHandSwingMessage, val -> ModConfig.get().showHandSwingMessage = val))
                .option(createBoolOption("remove_creative_tab_items", true, () -> ModConfig.get().removeItemsFromCreativeTabs, val -> ModConfig.get().removeItemsFromCreativeTabs = val))
                .option(createBoolOption("remove_emi_items", true, () -> ModConfig.get().removeItemsFromEmi, val -> ModConfig.get().removeItemsFromEmi = val))
                .option(createBoolOption("remove_inventory_items", true, () -> ModConfig.get().removeItemsFromInventories, val -> ModConfig.get().removeItemsFromInventories = val))
                .option(createBoolOption("remove_on_inventory_open", true, () -> ModConfig.get().removeItemsOnInventoryOpen, val -> ModConfig.get().removeItemsOnInventoryOpen = val))
                .option(createBoolOption("remove_dropped_items", true, () -> ModConfig.get().removeDroppedItems, val -> ModConfig.get().removeDroppedItems = val))
                .option(createBoolOption("remove_loot_items", true, () -> ModConfig.get().removeItemsFromLootChests, val -> ModConfig.get().removeItemsFromLootChests = val))
                .option(createBoolOption("remove_storage_items", true, () -> ModConfig.get().removeItemsFromStorage, val -> ModConfig.get().removeItemsFromStorage = val))
                .option(createBoolOption("remove_trades", true, () -> ModConfig.get().removeItemsFromTrades, val -> ModConfig.get().removeItemsFromTrades = val))
                .option(createBoolOption("remove_info_tabs", true, () -> ModConfig.get().removeItemsFromInfoTabs, val -> ModConfig.get().removeItemsFromInfoTabs = val))
                .option(createBoolOption("remove_mob_equipment", true, () -> ModConfig.get().removeMobEquipment, val -> ModConfig.get().removeMobEquipment = val))
                .option(createBoolOption("remove_cnm_children", true, () -> ModConfig.get().removeCnmChildren, val -> ModConfig.get().removeCnmChildren = val));

        ConfigCategory.Builder blacklist = ConfigCategory.createBuilder()
                .name(Component.translatable("config.reliable_remover.category.blacklist"))
                .group(ListOption.<String>createBuilder()
                        .name(Component.translatable("config.reliable_remover.option.blacklisted_items"))
                        .description(OptionDescription.of(Component.translatable("config.reliable_remover.option.blacklisted_items.tooltip")))
                        .binding(new ArrayList<>(), () -> ModConfig.get().blacklistedItems, val -> ModConfig.get().blacklistedItems = new ArrayList<>(val))
                        .controller(StringControllerBuilder::create)
                        .initial("")
                        .build());

        ConfigCategory.Builder emi = ConfigCategory.createBuilder()
                .name(Component.translatable("config.reliable_remover.category.emi"))
                .option(createBoolOption("enable_emi_removal", false, () -> ModConfig.get().enableEmiRemoval, val -> ModConfig.get().enableEmiRemoval = val))
                .option(createBoolOption("show_toast", true, () -> ModConfig.get().showEmiToast, val -> ModConfig.get().showEmiToast = val))
                .option(createBoolOption("show_chat_messages", true, () -> ModConfig.get().showEmiChatMessages, val -> ModConfig.get().showEmiChatMessages = val))
                .option(createBoolOption("reload_emi", false, () -> ModConfig.get().reloadAfterRemoval, val -> ModConfig.get().reloadAfterRemoval = val));

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
