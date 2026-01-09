package com.evandev.reliable_remover.mixin.minecraft;

import com.evandev.reliable_remover.config.ModConfig;
import com.evandev.reliable_remover.config.RuleManager;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin extends Player {

    public ServerPlayerMixin(Level level, BlockPos pos, float yRot, GameProfile gameProfile) {
        super(level, pos, yRot, gameProfile);
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void reliable_remover$tick(CallbackInfo ci) {
        if (ModConfig.get().removeItemsFromInventories && this.tickCount % 20 == 0) {
            for (int i = 0; i < this.getInventory().getContainerSize(); i++) {
                ItemStack stack = this.getInventory().getItem(i);
                if (!stack.isEmpty() && RuleManager.isHidden(stack, this.level())) {
                    stack.setCount(0);
                    if (ModConfig.get().showRemovalMessage) {
                        this.displayClientMessage(Component.translatable("message.reliable_remover.item_removed"), true);
                    }
                }
            }
        }
    }

    @Inject(method = "attack", at = @At("HEAD"), cancellable = true)
    private void reliable_remover$attack(Entity target, CallbackInfo ci) {
        if (RuleManager.isAttackBlocked(this.getMainHandItem(), this.level(), target)) {
            this.displayClientMessage(Component.translatable("message.reliable_remover.attack_disabled"), true);
            ci.cancel();
        }
    }

    @Inject(method = "swing", at = @At("HEAD"), cancellable = true)
    private void reliable_remover$cancelSwing(InteractionHand hand, CallbackInfo ci) {
        if (RuleManager.isAttackBlocked(this.getMainHandItem(), this.level(), null)) {
            ci.cancel();
        }
    }
}