package com.evandev.reliable_remover.mixin.minecraft;

import com.evandev.reliable_remover.config.ModConfig;
import com.evandev.reliable_remover.config.RuleManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {

    @Shadow
    public LocalPlayer player;
    @Shadow
    public HitResult hitResult;

    @Inject(method = "startAttack", at = @At("HEAD"), cancellable = true)
    private void reliable_remover$onStartAttack(CallbackInfoReturnable<Boolean> cir) {
        if (this.player == null) return;

        if (RuleManager.isHandSwingBlocked(this.player.getMainHandItem(), this.player.level())) {
            if (ModConfig.get().showHandSwingMessage) {
                this.player.sendSystemMessage(Component.translatable("message.reliable_remover.swing_disabled"));
            }
            cir.setReturnValue(false);
            return;
        }

        if (this.hitResult != null && this.hitResult.getType() == HitResult.Type.ENTITY) {
            EntityHitResult entityHit = (EntityHitResult) this.hitResult;
            if (RuleManager.isAttackBlocked(this.player.getMainHandItem(), this.player.level(), entityHit.getEntity())) {
                if (ModConfig.get().showAttackMessage) {
                    this.player.sendSystemMessage(Component.translatable("message.reliable_remover.attack_disabled"));
                }
                cir.setReturnValue(false);
            }
        }
    }
}