package com.evandev.reliable_remover.mixin.client;

//? if >=1.21 {
import com.evandev.reliable_remover.config.RuleManager;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.alchemy.PotionContents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Strips blocked effects from effect tooltips and swaps replaced ones for their replacement
 */
@Mixin(PotionContents.class)
public class PotionTooltipMixin {

    @Inject(method = "addPotionTooltip(Ljava/lang/Iterable;Ljava/util/function/Consumer;FF)V", at = @At("HEAD"), cancellable = true)
    private static void reliable_remover$filterBlockedEffects(Iterable<MobEffectInstance> effects, Consumer<Component> lines, float durationScale, float tickrate, CallbackInfo ci) {
        var player = Minecraft.getInstance().player;
        var level = player != null ? player.level() : null;
        List<MobEffectInstance> filtered = new ArrayList<>();
        boolean changed = false;
        for (MobEffectInstance effect : effects) {
            var replacement = RuleManager.getEffectReplacement(effect.getEffect(), level, player);
            if (replacement != null) {
                changed = true;
                filtered.add(new MobEffectInstance(replacement, effect.getDuration(), effect.getAmplifier(), effect.isAmbient(), effect.isVisible(), effect.showIcon()));
            } else if (RuleManager.isEffectBlocked(effect.getEffect(), level, player)) {
                changed = true;
            } else {
                filtered.add(effect);
            }
        }
        if (!changed) return;

        ci.cancel();
        if (!filtered.isEmpty()) {
            PotionContents.addPotionTooltip(filtered, lines, durationScale, tickrate);
        }
    }
}
//?} else {
/*import com.evandev.reliable_remover.config.RuleManager;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.alchemy.PotionUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

@Mixin(PotionUtils.class)
public class PotionTooltipMixin {

    @Inject(method = "addPotionTooltip(Ljava/util/List;Ljava/util/List;F)V", at = @At("HEAD"), cancellable = true)
    private static void reliable_remover$filterBlockedEffects(List<MobEffectInstance> effects, List<Component> tooltips, float durationFactor, CallbackInfo ci) {
        var player = Minecraft.getInstance().player;
        var level = player != null ? player.level() : null;
        List<MobEffectInstance> filtered = new ArrayList<>();
        boolean changed = false;
        for (MobEffectInstance effect : effects) {
            var replacement = RuleManager.getEffectReplacement(effect.getEffect(), level, player);
            if (replacement != null) {
                changed = true;
                filtered.add(new MobEffectInstance(replacement.value(), effect.getDuration(), effect.getAmplifier(), effect.isAmbient(), effect.isVisible(), effect.showIcon()));
            } else if (RuleManager.isEffectBlocked(effect.getEffect(), level, player)) {
                changed = true;
            } else {
                filtered.add(effect);
            }
        }
        if (!changed) return;

        ci.cancel();
        if (!filtered.isEmpty()) {
            PotionUtils.addPotionTooltip(filtered, tooltips, durationFactor);
        }
    }
}
*///?}
