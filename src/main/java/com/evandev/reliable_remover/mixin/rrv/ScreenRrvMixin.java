package com.evandev.reliable_remover.mixin.rrv;

//? if >=26.1 {
import com.evandev.reliable_remover.client.Keybinds;
import com.evandev.reliable_remover.compat.ReliableRemoverRrvPlugin;
import com.moulberry.mixinconstraints.annotations.IfMinecraftVersion;
import com.moulberry.mixinconstraints.annotations.IfModLoaded;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@IfMinecraftVersion(minVersion = "26.1")
@IfModLoaded("rrv")
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
//?} else {
/*import com.moulberry.mixinconstraints.annotations.IfMinecraftVersion;
import com.moulberry.mixinconstraints.annotations.IfModLoaded;
import org.spongepowered.asm.mixin.Mixin;

@IfMinecraftVersion(minVersion = "26.1")
@IfModLoaded("rrv")
@Mixin(targets = {"net.minecraft.client.gui.screens.Screen", "net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen"}, remap = false)
public class ScreenRrvMixin {
}
*///?}
