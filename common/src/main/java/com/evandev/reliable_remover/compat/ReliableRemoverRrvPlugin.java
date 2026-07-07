package com.evandev.reliable_remover.compat;

import cc.cassian.rrv.api.ReliableRecipeViewerClientPlugin;
import cc.cassian.rrv.api.overlay.OverlayView;
import cc.cassian.rrv.api.recipe.ItemView;
import cc.cassian.rrv.common.overlay.ItemSlot;
import cc.cassian.rrv.common.overlay.itemlist.panel.SidePanelOverlay;
import cc.cassian.rrv.common.overlay.itemlist.view.ItemViewOverlay;
import cc.cassian.rrv.common.recipe.ItemViewRecipes;
import cc.cassian.rrv.common.recipe.inventory.SlotContent;
import com.evandev.reliable_recipes.client.SharedToastOverlay;
import com.evandev.reliable_remover.client.Keybinds;
import com.evandev.reliable_remover.config.ModConfig;
import com.evandev.reliable_remover.config.RuleConfigIO;
import com.evandev.reliable_remover.config.RuleManager;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.item.ItemStack;

public class ReliableRemoverRrvPlugin implements ReliableRecipeViewerClientPlugin {

    public static void init() {
        ItemView.addClientReloadCallback(() -> {
            ModConfig.get();
            RuleManager.load();

            if (!ModConfig.get().removeItemsFromRrv) return;

            BuiltInRegistries.ITEM.forEach(item -> {
                if (RuleManager.isCreativeBlocked(item.getDefaultInstance()) || RuleManager.isHidden(item.getDefaultInstance())) {
                    ItemView.excludeItem(item);
                }
            });

            ItemViewRecipes.INFO_RECIPES.removeIf(recipe -> {
                for (SlotContent ingredient : recipe.getIngredients()) {
                    for (ItemStack stack : ingredient.getValidContents()) {
                        if (RuleManager.isInfoBlocked(stack) || RuleManager.isCreativeBlocked(stack) || RuleManager.isHidden(stack)) {
                            return true;
                        }
                    }
                }
                return false;
            });
        });

        OverlayView.registerGlobalOverlayKeybindSlotHandler(Keybinds.RRV_DELETE_KEY, (_, slot, overlayId) -> {
            return processDeleteKey(slot.getStack(), overlayId);
        });
    }

    public static boolean processDeleteKey(ItemStack stack, Identifier overlayId) {
        if (!ModConfig.get().enableRrvRemoval || stack.isEmpty()) return false;

        String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        if (ModConfig.get().blacklistedItems.contains(id)) return false;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return false;

        if (!mc.player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)) {
            mc.player.sendSystemMessage(Component.translatable("toast.reliable_remover.permission_denied"));
            return true;
        }

        mc.player.connection.sendCommand("rremover remove " + id);
        RuleConfigIO.addRemovalRule(id);
        RuleManager.load();

        ItemView.excludeItem(stack.getItem());

        if (OverlayView.ITEM_VIEW.equals(overlayId)) {
            ItemViewOverlay.INSTANCE.availableItems().removeIf(s -> s.getItem() == stack.getItem());
            ItemViewOverlay.INSTANCE.updateSlots();
        } else if (OverlayView.BOOKMARKS.equals(overlayId) || OverlayView.CRAFTABLES.equals(overlayId)) {
            SidePanelOverlay.INSTANCE.availableItems().removeIf(s -> s.getItem() == stack.getItem());
            SidePanelOverlay.INSTANCE.updateSlots();
        }

        if (ModConfig.get().showRrvToast) {
            SharedToastOverlay.show(Component.literal("Item Deleted"), Component.literal(id), stack);
        }

        if (ModConfig.get().showRrvChatMessages) {
            Component undoButton = Component.translatable("toast.reliable_remover.undo")
                    .withStyle(Style.EMPTY
                            .withColor(ChatFormatting.GOLD)
                            .withClickEvent(new ClickEvent.RunCommand("/rremover undo " + id))
                            .withHoverEvent(new HoverEvent.ShowText(Component.literal("Click to restore item"))));

            mc.player.sendSystemMessage(Component.translatable("toast.reliable_remover.deleted", id)
                    .append(" ").append(undoButton));
        }

        return true;
    }

    /**
     * Intercept method to force a deletion if the text field attempts to steal the focus key.
     */
    public static boolean tryDeleteHoveredRrvItem() {
        if (!ModConfig.get().enableRrvRemoval) return false;

        try {
            if (ItemViewOverlay.INSTANCE.isEnabled()) {
                for (ItemSlot slot : ItemViewOverlay.INSTANCE.itemSlots()) {
                    if (slot.isHovered() && !slot.getStack().isEmpty()) {
                        return processDeleteKey(slot.getStack(), OverlayView.ITEM_VIEW);
                    }
                }
            }

            if (SidePanelOverlay.INSTANCE.isEnabled()) {
                for (ItemSlot slot : SidePanelOverlay.INSTANCE.itemSlots()) {
                    if (slot.isHovered() && !slot.getStack().isEmpty()) {
                        return processDeleteKey(slot.getStack(), OverlayView.BOOKMARKS);
                    }
                }
            }
        } catch (Exception ignored) {
        }

        return false;
    }

    @Override
    public void onIntegrationInitialize() {
        init();
    }
}