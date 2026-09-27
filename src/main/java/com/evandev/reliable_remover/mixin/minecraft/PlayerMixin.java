package com.evandev.reliable_remover.mixin.minecraft;

import com.evandev.reliable_remover.config.ModConfig;
import com.evandev.reliable_remover.util.PlayerMessages;
import com.evandev.reliable_remover.config.RuleManager;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public class PlayerMixin {

    @Inject(method = "attack", at = @At("HEAD"), cancellable = true)
    private void reliable_remover$attack(Entity entity, CallbackInfo ci) {
        Player player = (Player) (Object) this;
        if (RuleManager.isAttackBlocked(player.getMainHandItem(), player.level(), entity)) {
            if (ModConfig.get().showAttackMessage) {
                PlayerMessages.actionBar(player, Component.translatable("message.reliable_remover.attack_disabled"));
            }
            ci.cancel();
        }
    }

    @Inject(method = "interactOn", at = @At("HEAD"), cancellable = true)
    //? if >=26.1 {
    private void reliable_remover$cancelInteractOn(Entity entityToInteractOn, InteractionHand hand, Vec3 location, CallbackInfoReturnable<InteractionResult> cir) {
    //?} else {
    /*private void reliable_remover$cancelInteractOn(Entity entityToInteractOn, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
    *///?}
        Player player = (Player) (Object) this;
        ItemStack stack = player.getItemInHand(hand);
        if (RuleManager.isInteractionBlocked(stack, player.level(), entityToInteractOn)) {
            if (ModConfig.get().showRemovalMessage) {
                PlayerMessages.actionBar(player, Component.translatable("message.reliable_remover.interaction_disabled"));
            }
            cir.setReturnValue(InteractionResult.FAIL);
        }
    }
}