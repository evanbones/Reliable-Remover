package com.evandev.reliable_remover.mixin.rrv;

import cc.cassian.rrv.api.recipe.ItemView;
import cc.cassian.rrv.common.overlay.ItemSlot;
import cc.cassian.rrv.common.overlay.itemlist.AbstractRrvItemListOverlay;
import cc.cassian.rrv.common.overlay.itemlist.panel.SidePanelOverlay;
import cc.cassian.rrv.common.overlay.itemlist.view.ItemViewOverlay;
import com.evandev.reliable_recipes.client.SharedToastOverlay;
import com.evandev.reliable_remover.client.Keybinds;
import com.evandev.reliable_remover.config.ModConfig;
import com.evandev.reliable_remover.config.RuleConfigIO;
import com.evandev.reliable_remover.config.RuleManager;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = {ItemViewOverlay.class, SidePanelOverlay.class}, remap = false)
public abstract class RrvOverlaysMixin {

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void reliable_remover$onKeyPressed(KeyEvent event, CallbackInfoReturnable<Boolean> cir) {
        if (!ModConfig.get().enableRrvRemoval) return;

        AbstractRrvItemListOverlay overlay = (AbstractRrvItemListOverlay) (Object) this;

        if (Keybinds.RRV_DELETE_KEY.matches(event)) {

            for (ItemSlot slot : overlay.itemSlots()) {
                if (slot.isHovered()) {
                    ItemStack stack = slot.getStack();
                    String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();

                    if (!ModConfig.get().blacklistedItems.contains(id)) {
                        Minecraft mc = Minecraft.getInstance();

                        if (mc.player != null) {
                            if (!mc.player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)) {
                                mc.player.sendSystemMessage(Component.translatable("toast.reliable_remover.permission_denied"));
                            } else {
                                mc.player.connection.sendCommand("rremover remove " + id);
                                RuleConfigIO.addRemovalRule(id);
                                RuleManager.load();

                                ItemView.excludeItem(stack.getItem());

                                overlay.availableItems().removeIf(s -> s.getItem() == stack.getItem());
                                overlay.updateSlots();

                                if (ModConfig.get().showRrvToast) {
                                    SharedToastOverlay.show(
                                            Component.literal("Item Deleted"),
                                            Component.literal(id),
                                            stack
                                    );
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
                            }
                        }
                    }

                    cir.setReturnValue(true);
                    return;
                }
            }
        }
    }
}