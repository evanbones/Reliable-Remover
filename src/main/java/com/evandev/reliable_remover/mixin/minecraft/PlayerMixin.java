package com.evandev.reliable_remover.mixin.minecraft;

import com.evandev.reliable_remover.config.ModConfig;
import com.evandev.reliable_remover.config.RuleManager;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public class PlayerMixin {

    @Inject(method = "attack", at = @At("HEAD"), cancellable = true)
    private void reliable_remover$attack(Entity entity, CallbackInfo ci) {
        Player player = (Player) (Object) this;
        if (RuleManager.isAttackBlocked(player.getMainHandItem(), player.level(), entity)) {
            if (ModConfig.get().showAttackMessage) {
                player.sendSystemMessage(Component.translatable("message.reliable_remover.attack_disabled"));
            }
            ci.cancel();
        }
    }
}