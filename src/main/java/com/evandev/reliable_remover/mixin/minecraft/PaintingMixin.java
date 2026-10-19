package com.evandev.reliable_remover.mixin.minecraft;

import com.evandev.reliable_remover.config.RuleManager;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.Holder;
//? if >=26.1 {
import net.minecraft.world.entity.decoration.painting.Painting;
import net.minecraft.world.entity.decoration.painting.PaintingVariant;
//?} else {
/*import net.minecraft.world.entity.decoration.Painting;
import net.minecraft.world.entity.decoration.PaintingVariant;
*///?}
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.function.Consumer;

@Mixin(Painting.class)
public abstract class PaintingMixin {

    @WrapOperation(method = "create", at = @At(value = "INVOKE", target = "Ljava/lang/Iterable;forEach(Ljava/util/function/Consumer;)V", remap = false))
    private static void reliable_remover$skipRemovedPaintings(Iterable<Holder<PaintingVariant>> variants, Consumer<? super Holder<PaintingVariant>> action, Operation<Void> original) {
        Consumer<Holder<PaintingVariant>> filtered = variant -> {
            if (!RuleManager.isPaintingBlocked(variant)) action.accept(variant);
        };
        original.call(variants, filtered);
    }
}
