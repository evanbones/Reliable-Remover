package com.evandev.reliable_remover.mixin.minecraft;

import com.evandev.reliable_remover.config.RuleManager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootTable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.function.Consumer;

@Mixin(LootTable.class)
public class LootTableMixin {

    @ModifyVariable(
            method = "getRandomItemsRaw(Lnet/minecraft/world/level/storage/loot/LootContext;Ljava/util/function/Consumer;)V",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 0
    )
    private Consumer<ItemStack> reliable_remover$wrapConsumer(Consumer<ItemStack> original) {
        return stack -> {
            ItemStack replacement = RuleManager.getLootReplacement(stack, null);
            if (replacement != null) {
                original.accept(replacement);
            } else if (!RuleManager.isLootBlocked(stack, null)) {
                original.accept(stack);
            }
        };
    }
}