package com.evandev.reliable_remover.mixin.minecraft;

import com.evandev.reliable_remover.config.ModConfig;
import com.evandev.reliable_remover.config.RuleManager;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.Arrays;

@Mixin(LootPool.class)
public class LootPoolMixin {

    @ModifyVariable(method = "<init>", at = @At("HEAD"), argsOnly = true)
    private static LootPoolEntryContainer[] reliable_remover$filterEntries(LootPoolEntryContainer[] entries) {
        if (!ModConfig.get().removeItemsFromLootChests) {
            return entries;
        }

        return Arrays.stream(entries)
                .filter(entry -> {
                    if (entry instanceof LootItem lootItem) {
                        Item item = ((LootItemAccessor) lootItem).getReliableRemoverItem();
                        return !RuleManager.isHidden(new ItemStack(item));
                    }
                    return true;
                })
                .toArray(LootPoolEntryContainer[]::new);
    }
}