package com.evandev.reliable_remover.mixin.minecraft.loot;

import com.evandev.reliable_remover.config.RuleManager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.EnchantRandomlyFunction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EnchantRandomlyFunction.class)
public class EnchantRandomlyFunctionMixin {

    @Inject(method = "run", at = @At("RETURN"))
    private void reliable_remover$stripBlockedEnchantments(ItemStack stack, LootContext context, CallbackInfoReturnable<ItemStack> cir) {
        ItemStack result = cir.getReturnValue();
        if (result != null && !result.isEmpty()) {
            RuleManager.stripBlockedEnchantments(result);
        }
    }
}
