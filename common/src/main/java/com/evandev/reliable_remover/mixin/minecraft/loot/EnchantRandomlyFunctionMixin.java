package com.evandev.reliable_remover.mixin.minecraft.loot;

import com.evandev.reliable_remover.config.RuleManager;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.EnchantRandomlyFunction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EnchantRandomlyFunction.class)
public class EnchantRandomlyFunctionMixin {

    @Inject(method = "run", at = @At("RETURN"), cancellable = true)
    private void reliable_remover$cleanAndRerollRandomEnchantment(ItemStack originalStack, LootContext context, CallbackInfoReturnable<ItemStack> cir) {
        ItemStack stack = cir.getReturnValue();
        if (stack == null || stack.isEmpty()) return;

        RuleManager.stripBlockedEnchantments(stack, context != null ? context.getLevel() : null);

        if (stack.is(Items.ENCHANTED_BOOK)) {
            ItemEnchantments stored = stack.get(DataComponents.STORED_ENCHANTMENTS);
            if (stored == null || stored.isEmpty()) {
                cir.setReturnValue(new ItemStack(Items.BOOK, stack.getCount()));
            }
        }
    }
}
