package com.evandev.reliable_remover.mixin.minecraft;

import com.evandev.reliable_remover.config.ModConfig;
import com.evandev.reliable_remover.config.RuleManager;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemEntity.class)
public abstract class ItemEntityMixin extends Entity {

    public ItemEntityMixin(EntityType<?> type, Level level) {
        super(type, level);
    }

    @Shadow public abstract ItemStack getItem();

    @Inject(method = "tick", at = @At("HEAD"))
    private void reliable_remover$tick(CallbackInfo ci) {
        if (!ModConfig.get().removeDroppedItems) return;

        if (!this.level().isClientSide() && this.tickCount % 20 == 0) {
            ItemStack stack = this.getItem();
            if (!stack.isEmpty() && RuleManager.isHidden(stack)) {
                this.discard();
            }
        }
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void reliable_remover$checkLoad(ValueInput input, CallbackInfo ci) {
        if (ModConfig.get().removeDroppedItems && RuleManager.isHidden(this.getItem())) {
            this.discard();
        }
    }
}