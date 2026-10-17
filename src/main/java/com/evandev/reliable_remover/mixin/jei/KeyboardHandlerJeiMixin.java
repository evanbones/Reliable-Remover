package com.evandev.reliable_remover.mixin.jei;

import com.evandev.reliable_remover.client.Keybinds;
import com.evandev.reliable_remover.compat.JeiInteractions;
import com.moulberry.mixinconstraints.annotations.IfModLoaded;
import net.minecraft.client.KeyboardHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//? if >=26.1 {
import net.minecraft.client.input.KeyEvent;
//?}

@IfModLoaded("jei")
@Mixin(KeyboardHandler.class)
public class KeyboardHandlerJeiMixin {

    //? if >=26.1 {
    @Inject(method = "keyPress", at = @At("HEAD"), cancellable = true)
    private void reliable_remover$interceptJeiDelete(long handle, int action, KeyEvent event, CallbackInfo ci) {
        if (action == 1 && Keybinds.JEI_DELETE_KEY.matches(event) && JeiInteractions.tryDeleteHovered()) {
            ci.cancel();
        }
    }
    //?} else {
    /*@Inject(method = "keyPress", at = @At("HEAD"), cancellable = true)
    private void reliable_remover$interceptJeiDelete(long window, int key, int scanCode, int action, int modifiers, CallbackInfo ci) {
        if (action == 1 && Keybinds.JEI_DELETE_KEY.matches(key, scanCode) && JeiInteractions.tryDeleteHovered()) {
            ci.cancel();
        }
    }
    *///?}
}
