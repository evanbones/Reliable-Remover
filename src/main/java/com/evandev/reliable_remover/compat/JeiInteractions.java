package com.evandev.reliable_remover.compat;

import com.evandev.reliable_recipes.client.SharedToastOverlay;
import com.evandev.reliable_remover.client.ServerCommands;
import com.evandev.reliable_remover.config.ModConfig;
import com.evandev.reliable_remover.config.RuleManager;
import com.evandev.reliable_remover.util.PlayerMessages;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.world.item.ItemStack;
//? if >=26.1 {
import net.minecraft.server.permissions.Permissions;
//?}

public class JeiInteractions {

    public static boolean tryDeleteHovered() {
        if (!ModConfig.get().enableJeiRemoval) return false;

        IJeiRuntime runtime = ReliableRemoverJeiPlugin.getRuntime();
        if (runtime == null || runtime.getIngredientListOverlay().hasKeyboardFocus()) return false;
        if (Minecraft.getInstance().mouseHandler.isMouseGrabbed()) return false;

        ItemStack stack = runtime.getIngredientListOverlay().getIngredientUnderMouse()
                .or(() -> runtime.getBookmarkOverlay().getIngredientUnderMouse())
                .flatMap(ingredient -> ingredient.getIngredient(VanillaTypes.ITEM_STACK))
                .orElse(ItemStack.EMPTY);
        if (stack.isEmpty()) return false;

        String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        if (RuleManager.getBlacklistedItems().contains(id)) return false;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return false;

        //? if >=26.1 {
        boolean hasPermission = mc.player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);
        //?} else {
        /*boolean hasPermission = mc.player.hasPermissions(2);
        *///?}
        if (!hasPermission) {
            PlayerMessages.actionBar(mc.player, Component.translatable("toast.reliable_remover.permission_denied"));
            return true;
        }

        if (!ServerCommands.canRemoveItems()) {
            PlayerMessages.actionBar(mc.player, Component.translatable("toast.reliable_remover.server_missing"));
            return true;
        }

        //? if <1.19.3 {
        /*mc.player.commandSigned("rremover remove " + id, null);
        *///?} else {
        mc.player.connection.sendCommand("rremover remove " + id);
        //?}

        ReliableRemoverJeiPlugin.hideItem(stack);

        if (ModConfig.get().showJeiToast) {
            SharedToastOverlay.show(Component.literal("Item Deleted"), Component.literal(id), stack);
        }

        if (ModConfig.get().showJeiChatMessages) {
            String undoCommand = "/rremover undo " + id;
            Component undoHover = Component.literal("Click to restore item");
            Component undoButton = Component.translatable("toast.reliable_remover.undo")
                    .withStyle(Style.EMPTY
                            .withColor(ChatFormatting.GOLD)
                            //? if >=26.1 {
                            .withClickEvent(new ClickEvent.RunCommand(undoCommand))
                            .withHoverEvent(new HoverEvent.ShowText(undoHover)));
                            //?} else {
                            /*.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, undoCommand))
                            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, undoHover)));
                            *///?}

            mc.player.sendSystemMessage(Component.translatable("toast.reliable_remover.deleted", id)
                    .append(" ").append(undoButton));
        }

        return true;
    }
}
