package com.evandev.reliable_remover.mixin.emi;

//? if <=1.21.1 {
/*import com.evandev.reliable_remover.compat.EmiBlacklistHelper;
import com.evandev.reliable_remover.config.ModConfig;
import com.evandev.reliable_remover.config.RuleManager;
import com.moulberry.mixinconstraints.annotations.IfMinecraftVersion;
import com.moulberry.mixinconstraints.annotations.IfModLoaded;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.stack.ListEmiIngredient;
import dev.emi.emi.registry.EmiTags;
import dev.emi.emi.runtime.EmiTagKey;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.stream.Collectors;

@IfMinecraftVersion(maxVersion = "1.21.1", maxInclusive = true)
@IfModLoaded("emi")
@Mixin(value = EmiTags.class, remap = false)
public class EmiTagsMixin {

    @Inject(method = "getValues", at = @At("RETURN"), cancellable = true)
    private static void reliable_remover$filterEmiTags(EmiTagKey<?> key, CallbackInfoReturnable<List<EmiStack>> cir) {
        if (!ModConfig.get().removeItemsFromEmi) return;

        List<EmiStack> original = cir.getReturnValue();
        if (original == null || original.isEmpty()) return;

        List<EmiStack> filtered = original.stream()
                .filter(EmiTagsMixin::reliable_remover$isVisible)
                .collect(Collectors.toList());

        if (!filtered.isEmpty() && filtered.size() != original.size()) {
            cir.setReturnValue(filtered);
        }
    }

    @Inject(method = "getIngredient", at = @At("RETURN"), cancellable = true)
    private static <T> void reliable_remover$filterIngredientResult(Class<T> clazz, List<EmiStack> stacks, long amount, CallbackInfoReturnable<EmiIngredient> cir) {
        if (!ModConfig.get().removeItemsFromEmi) return;

        EmiIngredient result = cir.getReturnValue();
        if (result == null || result.getClass() != ListEmiIngredient.class) return;

        List<EmiStack> originalStacks = result.getEmiStacks();
        List<EmiStack> filtered = originalStacks.stream()
                .filter(EmiTagsMixin::reliable_remover$isVisible)
                .collect(Collectors.toList());

        if (filtered.size() != originalStacks.size()) {
            if (filtered.isEmpty()) {
                cir.setReturnValue(EmiStack.EMPTY);
            } else {
                cir.setReturnValue(new ListEmiIngredient(filtered, amount));
            }
        }
    }

    @Unique
    private static boolean reliable_remover$isVisible(EmiStack stack) {
        try {
            if (EmiBlacklistHelper.isEmiStackBlacklisted(stack)) return false;
            ItemStack itemStack = stack.getItemStack();
            return itemStack == null || itemStack.isEmpty() || !RuleManager.isCreativeBlocked(itemStack);
        } catch (Throwable t) {
            return true;
        }
    }
}
*///?} else {
import com.moulberry.mixinconstraints.annotations.IfMinecraftVersion;
import com.moulberry.mixinconstraints.annotations.IfModLoaded;
import org.spongepowered.asm.mixin.Mixin;

@IfMinecraftVersion(maxVersion = "1.21.1")
@IfModLoaded("emi")
@Mixin(targets = "dev.emi.emi.registry.EmiTags", remap = false)
public class EmiTagsMixin {
}
//?}
