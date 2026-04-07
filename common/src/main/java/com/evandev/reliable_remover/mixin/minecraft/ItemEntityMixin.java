package com.evandev.reliable_remover.mixin.minecraft;

import com.evandev.reliable_remover.config.RuleManager;
import com.evandev.reliable_remover.data.Action;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemEntity.class)
public abstract class ItemEntityMixin extends Entity {

    public ItemEntityMixin(EntityType<?> type, Level level) {
        super(type, level);
    }

    @Shadow
    public abstract ItemStack getItem();

    @ModifyVariable(method = "setItem", at = @At("HEAD"), argsOnly = true)
    private ItemStack reliable_remover$modifySetItem(ItemStack stack) {
        ItemStack replacement = RuleManager.getReplacement(stack, Action.REMOVE_DROPS, this.level(), this, "drops");

        if (replacement != null) {
            return replacement;
        } else if (RuleManager.isDropsBlocked(stack, this.level(), this)) {
            this.discard();
            return ItemStack.EMPTY;
        }

        return stack;
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void reliable_remover$tick(CallbackInfo ci) {
        if (!this.level().isClientSide && this.tickCount % 20 == 0) {
            ItemStack stack = this.getItem();
            if (!stack.isEmpty()) {
                ItemStack replacement = RuleManager.getReplacement(stack, Action.REMOVE_DROPS, this.level(), this, "drops");
                if (replacement != null) {
                    ((ItemEntity) (Object) this).setItem(replacement);
                } else if (RuleManager.isDropsBlocked(stack, this.level(), this)) {
                    this.discard();
                }
            }
        }
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void reliable_remover$checkLoad(net.minecraft.nbt.CompoundTag compound, CallbackInfo ci) {
        ItemStack stack = this.getItem();
        ItemStack replacement = RuleManager.getReplacement(stack, Action.REMOVE_DROPS, this.level(), this, "drops");
        if (replacement != null) {
            ((ItemEntity) (Object) this).setItem(replacement);
        } else if (RuleManager.isDropsBlocked(stack, this.level(), this)) {
            this.discard();
        }
    }
}