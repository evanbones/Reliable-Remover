package com.evandev.reliable_remover.mixin.client;

import com.evandev.reliable_recipes.client.SharedToastOverlay;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//? if >=26.1 {
import net.minecraft.client.gui.GuiGraphicsExtractor;

@Mixin(Screen.class)
public class ScreenMixin {
    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void reliable_remover$renderToast(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a, CallbackInfo ci) {
        SharedToastOverlay.extract(graphics);
    }
}
//?} else if >=1.20 {
/*import net.minecraft.client.gui.GuiGraphics;

@Mixin(Screen.class)
public class ScreenMixin {
    @Inject(method = "render", at = @At("TAIL"))
    private void reliable_remover$renderToast(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        SharedToastOverlay.extract(guiGraphics);
    }
}
*///?} else {
/*import com.mojang.blaze3d.vertex.PoseStack;

@Mixin(Screen.class)
public class ScreenMixin {
    @Inject(method = "render", at = @At("TAIL"))
    private void reliable_remover$renderToast(PoseStack poseStack, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        SharedToastOverlay.extract(poseStack);
    }
}
*///?}