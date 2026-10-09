package com.evandev.reliable_remover.mixin.minecraft;

import com.evandev.reliable_remover.config.ModConfig;
import com.evandev.reliable_remover.util.PlayerMessages;
import com.evandev.reliable_remover.config.RuleManager;
import com.evandev.reliable_remover.data.Action;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Arrays;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    @Unique
    private static final EquipmentSlot[] reliable_remover$SLOTS = EquipmentSlot.values();
    @Unique
    private final ItemStack[] reliable_remover$checkedEquipment = new ItemStack[reliable_remover$SLOTS.length];
    @Unique
    private int reliable_remover$checkedGeneration = -1;

    @Inject(method = "detectEquipmentUpdates", at = @At("TAIL"))
    private void reliable_remover$removeMobEquipment(CallbackInfo ci) {
        LivingEntity entity = (LivingEntity) (Object) this;
        if (entity instanceof Player) return;

        int generation = RuleManager.getRulesGeneration();
        if (generation != this.reliable_remover$checkedGeneration) {
            Arrays.fill(this.reliable_remover$checkedEquipment, null);
            this.reliable_remover$checkedGeneration = generation;
        }

        for (int i = 0; i < reliable_remover$SLOTS.length; i++) {
            EquipmentSlot slot = reliable_remover$SLOTS[i];
            ItemStack stack = entity.getItemBySlot(slot);
            if (stack.isEmpty() || stack == this.reliable_remover$checkedEquipment[i]) continue;

            ItemStack replacement = RuleManager.getReplacement(stack, Action.REMOVE_EQUIPMENT, entity.level(), entity, "equipment");
            if (replacement != null) {
                entity.setItemSlot(slot, replacement);
            } else if (RuleManager.isEquipmentBlocked(stack, entity.level(), entity)) {
                entity.setItemSlot(slot, ItemStack.EMPTY);
            }
            this.reliable_remover$checkedEquipment[i] = entity.getItemBySlot(slot);
        }
    }

    @Inject(method = "addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z", at = @At("HEAD"), cancellable = true)
    private void reliable_remover$preventBlockedEffect(MobEffectInstance newEffect, Entity source, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity entity = (LivingEntity) (Object) this;
        if (newEffect == null) return;

        var replacement = RuleManager.getEffectReplacement(newEffect.getEffect(), entity.level(), entity);
        if (replacement != null) {
            //? if >=1.21 {
            MobEffectInstance replaced = new MobEffectInstance(replacement, newEffect.getDuration(), newEffect.getAmplifier(), newEffect.isAmbient(), newEffect.isVisible(), newEffect.showIcon());
            //?} else {
            /*MobEffectInstance replaced = new MobEffectInstance(replacement.value(), newEffect.getDuration(), newEffect.getAmplifier(), newEffect.isAmbient(), newEffect.isVisible(), newEffect.showIcon());
            *///?}
            cir.setReturnValue(entity.addEffect(replaced, source));
        } else if (RuleManager.isEffectBlocked(newEffect.getEffect(), entity.level(), entity)) {
            cir.setReturnValue(false);
        }
    }

    //? if >=26.3 {
    /*@Inject(method = "swing(Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/item/component/SwingAnimation;Z)Z", at = @At("HEAD"), cancellable = true)
    private void reliable_remover$cancelSwing(InteractionHand hand, net.minecraft.world.item.component.SwingAnimation animation, boolean sendToSwingingEntity, CallbackInfoReturnable<Boolean> cir) {
        if (!((Object) this instanceof ServerPlayer player)) return;
        if (RuleManager.isHandSwingBlocked(player.getMainHandItem(), player.level())) {
            if (ModConfig.get().showHandSwingMessage) {
                PlayerMessages.actionBar(player, Component.translatable("message.reliable_remover.swing_disabled"));
            }
            cir.setReturnValue(false);
        }
    }
    *///?}
}
