package com.evandev.reliable_remover.mixin.minecraft;

import com.evandev.reliable_remover.config.RuleManager;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayer.class)
public class ServerPlayerMixin {
    @Inject(method = "tick", at = @At("TAIL"))
    private void reliable_remover$tick(CallbackInfo ci) {
        ServerPlayer player = (ServerPlayer) (Object) this;
        if (player.tickCount % 20 == 0) {
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                ItemStack stack = player.getInventory().getItem(i);
                if (!stack.isEmpty() && RuleManager.isHidden(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString())) {
                    stack.setCount(0);
                }
            }
        }
    }

    @Inject(method = "attack", at = @At("HEAD"), cancellable = true)
    private void reliable_remover$attack(Entity target, CallbackInfo ci) {
        Player player = (Player) (Object) this;
        String id = BuiltInRegistries.ITEM.getKey(player.getMainHandItem().getItem()).toString();

        if (RuleManager.isAttackBlocked(id)) {
            ci.cancel();
        }
    }
}