package com.evandev.reliable_remover.mixin.minecraft;

import com.evandev.reliable_remover.config.ModConfig;
import com.evandev.reliable_remover.config.RuleManager;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    @Inject(method = "tick", at = @At("TAIL"))
    private void reliable_remover$removeMobEquipment(CallbackInfo ci) {
        if (!ModConfig.get().removeMobEquipment) return;

        LivingEntity entity = (LivingEntity) (Object) this;
        if (entity.level().isClientSide || entity instanceof Player || entity.tickCount % 20 != 0) return;

        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack stack = entity.getItemBySlot(slot);
            if (!stack.isEmpty() && RuleManager.isHidden(stack, entity.level(), entity)) {
                entity.setItemSlot(slot, ItemStack.EMPTY);
            }
        }
    }
}