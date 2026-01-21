package com.evandev.reliable_remover.mixin.minecraft;

import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LootItem.class)
public interface LootItemAccessor {
    @Accessor("item")
    Item getReliableRemoverItem();
}