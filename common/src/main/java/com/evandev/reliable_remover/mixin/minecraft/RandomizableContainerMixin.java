package com.evandev.reliable_remover.mixin.minecraft;

import com.evandev.reliable_remover.config.RuleManager;
import com.evandev.reliable_remover.data.Action;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RandomizableContainerBlockEntity.class)
public abstract class RandomizableContainerMixin {

    @Inject(method = "createMenu", at = @At("RETURN"))
    private void reliable_remover$filterItemsOnOpen(int containerId, Inventory playerInventory, Player player, CallbackInfoReturnable<AbstractContainerMenu> cir) {
        if (cir.getReturnValue() == null) return;

        RandomizableContainerBlockEntity container = (RandomizableContainerBlockEntity) (Object) this;

        if (container.getLevel() == null) return;

        boolean changed = false;

        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);

            if (!stack.isEmpty()) {
                ItemStack replacement = RuleManager.getReplacement(stack, Action.REMOVE_STORAGE, container.getLevel(), null, "storage");
                if (replacement != null) {
                    container.setItem(i, replacement);
                    changed = true;
                } else if (RuleManager.isStorageBlocked(stack, container.getLevel())) {
                    container.setItem(i, ItemStack.EMPTY);
                    changed = true;
                }
            }
        }

        if (changed) {
            container.setChanged();
        }
    }
}