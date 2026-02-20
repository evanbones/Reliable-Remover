package com.evandev.reliable_remover.mixin.emi;

import com.evandev.reliable_remover.client.ItemDeletionToastOverlay;
import com.evandev.reliable_remover.config.ModConfig;
import dev.emi.emi.api.EmiApi;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.stack.EmiStackInteraction;
import dev.emi.emi.screen.EmiScreenManager;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = EmiScreenManager.class, remap = false)
public class EmiScreenManagerMixin {

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private static void reliable_remover$onKeyPressed(int keyCode, int scanCode, int modifiers, CallbackInfoReturnable<Boolean> cir) {
        if (!ModConfig.get().enableEmiRemoval) return;

        if (keyCode == GLFW.GLFW_KEY_DELETE || keyCode == GLFW.GLFW_KEY_BACKSPACE) {
            EmiStackInteraction hovered = EmiApi.getHoveredStack(true);

            if (hovered != null && !hovered.isEmpty()) {
                EmiIngredient ingredient = hovered.getStack();

                if (ingredient != null && !ingredient.getEmiStacks().isEmpty()) {
                    EmiStack emiStack = ingredient.getEmiStacks().getFirst();
                    ResourceLocation id = emiStack.getId();

                    if (id != null) {
                        Minecraft mc = Minecraft.getInstance();

                        if (mc.player != null) {
                            if (!mc.player.hasPermissions(2)) {
                                mc.player.displayClientMessage(Component.translatable("toast.reliable_remover.permission_denied"), true);
                            } else {
                                mc.player.connection.sendCommand("rremover remove " + id);

                                if (ModConfig.get().showEmiToast) {
                                    ItemDeletionToastOverlay.show(
                                            Component.literal(id.toString()),
                                            emiStack.getItemStack()
                                    );
                                }

                                if (ModConfig.get().reloadAfterRemoval) {
                                    mc.player.connection.sendCommand("reload");
                                }
                            }
                        }
                        cir.setReturnValue(true);
                    }
                }
            }
        }
    }
}