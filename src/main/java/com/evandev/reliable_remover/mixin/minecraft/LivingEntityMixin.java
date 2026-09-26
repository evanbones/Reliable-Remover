package com.evandev.reliable_remover.mixin.minecraft;

import com.evandev.reliable_remover.config.ModConfig;
import com.evandev.reliable_remover.config.RuleManager;
import com.evandev.reliable_remover.data.Action;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    @Inject(method = "tick", at = @At("TAIL"))
    private void reliable_remover$removeMobEquipment(CallbackInfo ci) {
        LivingEntity entity = (LivingEntity) (Object) this;
        if (entity.level().isClientSide() || entity instanceof Player || entity.tickCount % 20 != 0) return;

        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack stack = entity.getItemBySlot(slot);
            if (!stack.isEmpty()) {
                ItemStack replacement = RuleManager.getReplacement(stack, Action.REMOVE_EQUIPMENT, entity.level(), entity, "equipment");
                if (replacement != null) {
                    entity.setItemSlot(slot, replacement);
                } else if (RuleManager.isEquipmentBlocked(stack, entity.level(), entity)) {
                    entity.setItemSlot(slot, ItemStack.EMPTY);
                }
            }
        }
    }

    //? if >=26.3 {
    /*@Inject(method = "swing(Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/item/component/SwingAnimation;Z)Z", at = @At("HEAD"), cancellable = true)
    private void reliable_remover$cancelSwing(InteractionHand hand, net.minecraft.world.item.component.SwingAnimation animation, boolean sendToSwingingEntity, CallbackInfoReturnable<Boolean> cir) {
        if (!((Object) this instanceof ServerPlayer player)) return;
        if (RuleManager.isHandSwingBlocked(player.getMainHandItem(), player.level())) {
            if (ModConfig.get().showHandSwingMessage) {
                player.sendSystemMessage(Component.translatable("message.reliable_remover.swing_disabled"));
            }
            cir.setReturnValue(false);
        }
    }
    *///?}
}
