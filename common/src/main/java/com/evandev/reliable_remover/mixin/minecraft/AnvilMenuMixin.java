package com.evandev.reliable_remover.mixin.minecraft;

import com.evandev.reliable_remover.config.RuleManager;
import com.evandev.reliable_remover.data.Action;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AnvilMenu.class)
public class AnvilMenuMixin {

    @Inject(method = "createResult", at = @At("RETURN"))
    private void reliable_remover$filterAnvilResult(CallbackInfo ci) {
        AnvilMenu menu = (AnvilMenu) (Object) this;

        ItemStack resultStack = menu.getSlot(2).getItem();

        if (!resultStack.isEmpty()) {
            RuleManager.stripBlockedEnchantments(resultStack);
            ItemStack replacement = RuleManager.getReplacement(resultStack, Action.REMOVE, null, null, "item");
            if (replacement != null) {
                menu.getSlot(2).set(replacement);
            } else if (RuleManager.isHidden(resultStack)) {
                menu.getSlot(2).set(ItemStack.EMPTY);
            }
        }
    }
}