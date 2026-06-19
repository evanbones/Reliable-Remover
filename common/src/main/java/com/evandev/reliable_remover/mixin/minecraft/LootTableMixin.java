package com.evandev.reliable_remover.mixin.minecraft;

import com.evandev.reliable_remover.config.RuleManager;
import com.evandev.reliable_remover.mixin.minecraft.accessor.LootContextAccessor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootTable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@Mixin(LootTable.class)
public abstract class LootTableMixin {
    @Unique
    private boolean reliable_remover$isReRolling = false;

    @Shadow
    public abstract void getRandomItemsRaw(LootContext context, Consumer<ItemStack> output);

    @Inject(method = "getRandomItemsRaw(Lnet/minecraft/world/level/storage/loot/LootContext;Ljava/util/function/Consumer;)V", at = @At("HEAD"), cancellable = true)
    private void reliable_remover$redirectGetRandomItemsRaw(LootContext context, Consumer<ItemStack> originalOutput, CallbackInfo ci) {
        if (reliable_remover$isReRolling) {
            return;
        }

        ci.cancel();

        List<ItemStack> finalItems = new ArrayList<>();
        int attempts = 0;
        int targetCount = -1;

        while (attempts < 5) {
            List<ItemStack> currentRollItems = new ArrayList<>();
            reliable_remover$isReRolling = true;
            try {
                this.getRandomItemsRaw(context, currentRollItems::add);
            } finally {
                reliable_remover$isReRolling = false;
            }

            if (targetCount == -1) {
                targetCount = currentRollItems.size();
            }

            if (currentRollItems.isEmpty()) {
                break;
            }

            boolean anyBlocked = false;

            var params = ((LootContextAccessor) context).reliable_remover$getParams();

            for (ItemStack stack : currentRollItems) {
                ItemStack replacement = RuleManager.getLootReplacement(stack, params);
                if (replacement != null) {
                    finalItems.add(replacement);
                } else if (!RuleManager.isLootBlocked(stack, params)) {
                    finalItems.add(stack);
                } else {
                    anyBlocked = true;
                }
            }

            if (!anyBlocked || finalItems.size() >= targetCount) {
                break;
            }

            attempts++;
        }

        finalItems.forEach(originalOutput);
    }
}