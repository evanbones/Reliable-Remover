package com.evandev.reliable_remover.mixin.minecraft;

import com.evandev.reliable_remover.config.RuleManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin extends Player {

    public ServerPlayerMixin(net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos, float yRot, com.mojang.authlib.GameProfile gameProfile) {
        super(level, pos, yRot, gameProfile);
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void reliable_remover$tick(CallbackInfo ci) {
        if (this.tickCount % 20 == 0) {
            for (int i = 0; i < this.getInventory().getContainerSize(); i++) {
                ItemStack stack = this.getInventory().getItem(i);
                if (!stack.isEmpty() && RuleManager.isHidden(stack)) {
                    stack.setCount(0);
                    this.displayClientMessage(Component.literal("Illegal item removed."), true);
                }
            }
        }
    }

    @Inject(method = "attack", at = @At("HEAD"), cancellable = true)
    private void reliable_remover$attack(Entity target, CallbackInfo ci) {
        if (RuleManager.isAttackBlocked(this.getMainHandItem())) {
            this.displayClientMessage(Component.literal("Attacking with this item is disabled."), true);
            ci.cancel();
        }
    }

    @Override
    public void swing(@NotNull InteractionHand hand) {
        if (RuleManager.isAttackBlocked(this.getItemInHand(hand))) {
            return;
        }
        super.swing(hand);
    }
}