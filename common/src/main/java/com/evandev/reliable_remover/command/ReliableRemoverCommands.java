package com.evandev.reliable_remover.command;

import com.evandev.reliable_remover.Constants;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ReliableRemoverCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("reliable_remover")
                .then(Commands.literal("hand")
                        .executes(ReliableRemoverCommands::dumpHand))
                .then(Commands.literal("hotbar")
                        .executes(ReliableRemoverCommands::dumpHotbar))
                .then(Commands.literal("inventory")
                        .executes(ReliableRemoverCommands::dumpInventory))
        );
    }

    private static int dumpHand(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            ItemStack stack = player.getMainHandItem();

            if (stack.isEmpty()) {
                context.getSource().sendFailure(Component.translatable("command.reliable_remover.dump.hand_empty"));
                return 0;
            }

            List<String> items = List.of(getItemId(stack));
            sendCopyableMessage(context.getSource(), items, "command.reliable_remover.dump.title.hand");
            return 1;
        } catch (Exception e) {
            Constants.LOG.error("Failed to dump hand item", e);
            return 0;
        }
    }

    private static int dumpHotbar(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            List<String> items = new ArrayList<>();
            Inventory inventory = player.getInventory();

            for (int i = 0; i < 9; i++) {
                ItemStack stack = inventory.getItem(i);
                if (!stack.isEmpty()) {
                    items.add(getItemId(stack));
                }
            }

            ItemStack offhand = inventory.offhand.get(0);
            if (!offhand.isEmpty()) {
                items.add(getItemId(offhand));
            }

            if (items.isEmpty()) {
                context.getSource().sendFailure(Component.translatable("command.reliable_remover.dump.hotbar_empty"));
                return 0;
            }

            sendCopyableMessage(context.getSource(), items.stream().distinct().toList(), "command.reliable_remover.dump.title.hotbar");
            return items.size();
        } catch (Exception e) {
            Constants.LOG.error("Failed to dump hotbar items", e);
            return 0;
        }
    }

    private static int dumpInventory(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            List<String> items = new ArrayList<>();
            Inventory inventory = player.getInventory();

            for (int i = 0; i < inventory.getContainerSize(); i++) {
                ItemStack stack = inventory.getItem(i);
                if (!stack.isEmpty()) {
                    items.add(getItemId(stack));
                }
            }

            if (items.isEmpty()) {
                context.getSource().sendFailure(Component.translatable("command.reliable_remover.dump.inventory_empty"));
                return 0;
            }

            List<String> uniqueItems = items.stream().distinct().toList();

            sendCopyableMessage(context.getSource(), uniqueItems, "command.reliable_remover.dump.title.inventory");
            return uniqueItems.size();
        } catch (Exception e) {
            Constants.LOG.error("Failed to dump inventory items", e);
            return 0;
        }
    }

    private static String getItemId(ItemStack stack) {
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
    }

    private static void sendCopyableMessage(CommandSourceStack source, List<String> itemIds, String titleKey) {
        String jsonArray = itemIds.stream()
                .map(id -> "\"" + id + "\"")
                .collect(Collectors.joining(", ", "[", "]"));

        Component message = Component.literal(jsonArray)
                .withStyle(Style.EMPTY
                        .withColor(ChatFormatting.GREEN)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, jsonArray))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable("command.reliable_remover.dump.copy_tooltip")))
                );

        source.sendSuccess(() -> Component.translatable(titleKey).withStyle(ChatFormatting.GOLD).append(":"), false);
        source.sendSuccess(() -> message, false);
    }
}