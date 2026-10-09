package com.evandev.reliable_remover.command;

import com.evandev.reliable_recipes.config.ConfigSync;
import com.evandev.reliable_remover.Constants;
import com.evandev.reliable_remover.client.ClientUtil;
import com.evandev.reliable_remover.config.ModConfig;
import com.evandev.reliable_remover.config.RuleConfigIO;
import com.evandev.reliable_remover.platform.Services;
import com.evandev.reliable_remover.util.PlayerMessages;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
//? if >=26.1 {
import net.minecraft.server.permissions.Permissions;
//?}
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ReliableRemoverCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("rremover")
                .then(Commands.literal("hand")
                        .executes(ReliableRemoverCommands::dumpHand))
                .then(Commands.literal("hotbar")
                        .executes(ReliableRemoverCommands::dumpHotbar))
                .then(Commands.literal("inventory")
                        .executes(ReliableRemoverCommands::dumpInventory))
                .then(Commands.literal("remove")
                        //? if >=26.1 {
                        .requires(s -> s.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
                        //?} else {
                        /*.requires(s -> s.hasPermission(2))
                        *///?}
                        .then(Commands.argument("id", IdentifierArgument.id())
                                .executes(ReliableRemoverCommands::executeRemove)))
                .then(Commands.literal("undo")
                        //? if >=26.1 {
                        .requires(s -> s.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
                        //?} else {
                        /*.requires(s -> s.hasPermission(2))
                        *///?}
                        .then(Commands.argument("id", IdentifierArgument.id())
                                .executes(ReliableRemoverCommands::executeUndo)))
                .then(Commands.literal("folder")
                        //? if >=26.1 {
                        .requires(s -> s.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER) && isSingleplayerHost(s))
                        //?} else {
                        /*.requires(s -> s.hasPermission(2) && isSingleplayerHost(s))
                        *///?}
                        .executes(ReliableRemoverCommands::executeFolder))
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

            //? if >=26.1 {
            ItemStack offhand = inventory.player.getOffhandItem();
            //?} else {
            /*ItemStack offhand = inventory.offhand.get(0);
            *///?}
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

    private static int executeFolder(CommandContext<CommandSourceStack> context) {
        try {
            Path folder = Services.PLATFORM.getConfigDirectory().resolve("reliable_remover");
            Files.createDirectories(folder);
            ClientUtil.openFolder(folder);

            PlayerMessages.sendSuccess(context.getSource(), () ->
                    Component.literal("Opened Reliable Remover config folder.").withStyle(ChatFormatting.GREEN), false);
            return 1;
        } catch (Exception e) {
            Constants.LOG.error("Failed to open reliable remover folder", e);
            context.getSource().sendFailure(
                    Component.literal("Failed to open config folder. Check server logs."));
            return 0;
        }
    }

    private static boolean isSingleplayerHost(CommandSourceStack source) {
        MinecraftServer server = source.getServer();
        if (server.isDedicatedServer()) return false;

        ServerPlayer player = source.getPlayer();
        GameProfile host = server.getSingleplayerProfile();
        if (player == null || host == null) return false;

        //? if >=26.1 {
        return player.getUUID().equals(host.id());
        //?} else {
        /*return player.getUUID().equals(host.getId());
        *///?}
    }

    private static String getItemId(ItemStack stack) {
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
    }

    private static void sendCopyableMessage(CommandSourceStack source, List<String> itemIds, String titleKey) {
        String jsonArray = itemIds.stream()
                .map(id -> "\"" + id + "\"")
                .collect(Collectors.joining(", ", "[", "]"));

        //? if >=26.1 {
        Component message = Component.literal(jsonArray)
                .withStyle(Style.EMPTY
                        .withColor(ChatFormatting.GREEN)
                        .withClickEvent(new ClickEvent.CopyToClipboard(jsonArray))
                        .withHoverEvent(new HoverEvent.ShowText(Component.translatable("command.reliable_remover.dump.copy_tooltip")))
                );
        //?} else {
        /*Component message = Component.literal(jsonArray)
                .withStyle(Style.EMPTY
                        .withColor(ChatFormatting.GREEN)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, jsonArray))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable("command.reliable_remover.dump.copy_tooltip")))
                );
        *///?}

        PlayerMessages.sendSuccess(source, () -> Component.translatable(titleKey).withStyle(ChatFormatting.GOLD).append(":"), false);
        PlayerMessages.sendSuccess(source, () -> message, false);
    }

    private static int executeRemove(CommandContext<CommandSourceStack> context) {
        Identifier id = IdentifierArgument.getId(context, "id");
        String itemId = id.toString();

        if (RuleConfigIO.addRemovalRule(itemId)) {
            //? if <26.1 {
            /*if (ModConfig.get().showEmiChatMessages) {
                Component undoButton = Component.translatable("toast.reliable_remover.undo")
                        .withStyle(Style.EMPTY
                                .withColor(ChatFormatting.GOLD)
                                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/rremover undo " + itemId))
                                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Click to restore item"))));

                PlayerMessages.sendSuccess(context.getSource(), () -> Component.translatable("toast.reliable_remover.deleted", itemId)
                        .append(" ").append(undoButton), true);
            }
            *///?}

            ConfigSync.sendToAll(context.getSource().getServer());

            if (ModConfig.get().reloadAfterRemoval) {
                context.getSource().getServer().getCommands().performPrefixedCommand(context.getSource(), "reload");
            }
            return 1;
        }
        return 0;
    }

    private static int executeUndo(CommandContext<CommandSourceStack> context) {
        Identifier id = IdentifierArgument.getId(context, "id");
        String itemId = id.toString();

        if (RuleConfigIO.removeRemovalRule(itemId)) {
            PlayerMessages.sendSuccess(context.getSource(), () -> Component.translatable("toast.reliable_remover.restored_item", itemId), true);
            ConfigSync.sendToAll(context.getSource().getServer());

            if (ModConfig.get().reloadAfterRemoval) {
                context.getSource().getServer().getCommands().performPrefixedCommand(context.getSource(), "reload");
            }

            return 1;
        } else {
            context.getSource().sendFailure(Component.translatable("toast.reliable_remover.restored_item_failed", itemId));
            return 0;
        }
    }
}