package com.evandev.reliable_remover.mixin.rrv;

import com.evandev.reliable_remover.client.Keybinds;
import com.evandev.reliable_remover.compat.ReliableRemoverRrvPlugin;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({Screen.class, CreativeModeInventoryScreen.class})
public class ScreenRrvMixin {

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void reliable_remover$interceptRrvDelete(KeyEvent event, CallbackInfoReturnable<Boolean> cir) {
        if (Keybinds.RRV_DELETE_KEY.matches(event)) {
            if (ReliableRemoverRrvPlugin.tryDeleteHoveredRrvItem()) {
                cir.setReturnValue(true);
            }
        }
    }
}