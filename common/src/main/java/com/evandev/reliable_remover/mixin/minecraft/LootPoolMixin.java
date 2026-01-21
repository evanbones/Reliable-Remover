package com.evandev.reliable_remover.mixin.minecraft;

import com.evandev.reliable_remover.config.ModConfig;
import com.evandev.reliable_remover.config.RuleManager;
import net.minecraft.core.Holder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.List;

@Mixin(LootPool.class)
public class LootPoolMixin {

    @ModifyVariable(method = "<init>", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private static List<LootPoolEntryContainer> reliable_remover$filterEntries(List<LootPoolEntryContainer> entries) {
        if (!ModConfig.get().removeItemsFromLootChests) {
            return entries;
        }

        return entries.stream()
                .filter(entry -> {
                    if (entry instanceof LootItem lootItem) {
                        Holder<Item> item = ((LootItemAccessor) lootItem).getReliableRemoverItem();
                        return !RuleManager.isHidden(new ItemStack(item));
                    }
                    return true;
                })
                .toList();
    }
}