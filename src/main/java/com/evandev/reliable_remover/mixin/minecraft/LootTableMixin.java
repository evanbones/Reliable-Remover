package com.evandev.reliable_remover.mixin.minecraft;

import com.evandev.reliable_remover.config.RuleManager;
import com.evandev.reliable_remover.mixin.minecraft.accessor.LootContextAccessor;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LootTable.class)
public abstract class LootTableMixin {

    @Unique
    private static final ThreadLocal<Boolean> reliable_remover$isReRolling = ThreadLocal.withInitial(() -> false);

    @Shadow
    protected abstract ObjectArrayList<ItemStack> getRandomItems(LootContext context);

    @Inject(method = "fill", at = @At("HEAD"))
    private void reliable_remover$markChestFill(Container container, LootParams params, long optionalRandomSeed, CallbackInfo ci) {
        RuleManager.setInChestFill(true);
    }

    @Inject(method = "fill", at = @At("RETURN"))
    private void reliable_remover$unmarkChestFill(Container container, LootParams params, long optionalRandomSeed, CallbackInfo ci) {
        RuleManager.setInChestFill(false);
    }

    @Inject(
            method = "getRandomItems(Lnet/minecraft/world/level/storage/loot/LootContext;)Lit/unimi/dsi/fastutil/objects/ObjectArrayList;",
            at = @At("RETURN"),
            cancellable = true
    )
    private void reliable_remover$filterAndReroll(LootContext context, CallbackInfoReturnable<ObjectArrayList<ItemStack>> cir) {
        if (reliable_remover$isReRolling.get()) {
            return;
        }

        ObjectArrayList<ItemStack> currentItems = cir.getReturnValue();
        if (currentItems == null || currentItems.isEmpty()) {
            return;
        }

        int targetCount = currentItems.size();
        ObjectArrayList<ItemStack> finalItems = new ObjectArrayList<>();
        var params = ((LootContextAccessor) context).reliable_remover$getParams();
        int attempts = 0;

        while (attempts < 5) {
            boolean anyBlocked = false;
            finalItems.clear();

            for (ItemStack stack : currentItems) {
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
            if (attempts < 5) {
                reliable_remover$isReRolling.set(true);
                try {
                    currentItems = this.getRandomItems(context);
                } finally {
                    reliable_remover$isReRolling.set(false);
                }
            }
        }

        cir.setReturnValue(finalItems);
    }
}