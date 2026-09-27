package com.evandev.reliable_remover.mixin.emi;

//? if <26.1 {
/*import com.evandev.reliable_remover.config.RuleManager;
import com.moulberry.mixinconstraints.annotations.IfMinecraftVersion;
import com.moulberry.mixinconstraints.annotations.IfModLoaded;
import fzzyhmstrs.emi_loot.parser.LootTableParser;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

@IfMinecraftVersion(maxVersion = "1.21.1", maxInclusive = true)
@IfModLoaded("emi_loot")
@Mixin(LootTableParser.class)
public class LootTableParserMixin {

    @ModifyVariable(
            method = "parseItemEntry(ILnet/minecraft/world/item/ItemStack;Ljava/util/List;Ljava/util/List;Z)Ljava/util/List;",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 0
    )
    private static ItemStack reliable_remover$replaceEmiLootItem(ItemStack item) {
        ItemStack replacement = RuleManager.getLootReplacement(item, null);
        return replacement != null ? replacement : item;
    }

    @Inject(
            method = "parseItemEntry(ILnet/minecraft/world/item/ItemStack;Ljava/util/List;Ljava/util/List;Z)Ljava/util/List;",
            at = @At("RETURN"),
            cancellable = true
    )
    private static void reliable_remover$filterEmiLootItems(
            int weight,
            ItemStack item,
            List<LootItemFunction> functions,
            List<LootItemCondition> conditions,
            boolean parentIsAlternative,
            CallbackInfoReturnable<List<LootTableParser.ItemEntryResult>> cir
    ) {
        List<LootTableParser.ItemEntryResult> original = cir.getReturnValue();
        if (original == null || original.isEmpty()) return;

        List<LootTableParser.ItemEntryResult> filtered = new ArrayList<>(original.size());
        boolean changed = false;
        for (LootTableParser.ItemEntryResult result : original) {
            if (RuleManager.isLootBlocked(result.item(), null)) {
                changed = true;
            } else {
                filtered.add(result);
            }
        }

        if (changed) {
            cir.setReturnValue(filtered);
        }
    }
}
*///?} else {
import com.moulberry.mixinconstraints.annotations.IfMinecraftVersion;
import com.moulberry.mixinconstraints.annotations.IfModLoaded;
import org.spongepowered.asm.mixin.Mixin;

@IfMinecraftVersion(maxVersion = "1.21.1")
@IfModLoaded("emi_loot")
@Mixin(targets = "fzzyhmstrs.emi_loot.parser.LootTableParser", remap = false)
public class LootTableParserMixin {
}
//?}
