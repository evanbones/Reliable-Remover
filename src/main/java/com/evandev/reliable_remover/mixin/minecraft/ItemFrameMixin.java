package com.evandev.reliable_remover.mixin.minecraft;

import com.evandev.reliable_remover.config.RuleManager;
import com.evandev.reliable_remover.data.Action;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ItemFrame.class)
public abstract class ItemFrameMixin {

    // Covers both placing an item and loading a frame, since readAdditionalSaveData calls setItem
    @ModifyVariable(method = "setItem(Lnet/minecraft/world/item/ItemStack;Z)V", at = @At("HEAD"), argsOnly = true)
    private ItemStack reliable_remover$modifySetItem(ItemStack itemStack) {
        ItemFrame entity = (ItemFrame) (Object) this;
        if (itemStack.isEmpty() || entity.level().isClientSide()) return itemStack;

        ItemStack replacement = RuleManager.getReplacement(itemStack, Action.REMOVE_EQUIPMENT, entity.level(), entity, "equipment");
        if (replacement != null) {
            return replacement;
        } else if (RuleManager.isEquipmentBlocked(itemStack, entity.level(), entity)) {
            return ItemStack.EMPTY;
        }

        return itemStack;
    }
}
