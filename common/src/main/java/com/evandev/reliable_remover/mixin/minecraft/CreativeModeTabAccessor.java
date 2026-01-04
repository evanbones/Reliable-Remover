package com.evandev.reliable_remover.mixin.minecraft;

import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Collection;
import java.util.Set;

@Mixin(CreativeModeTab.class)
public interface CreativeModeTabAccessor {
    @Accessor("displayItems")
    Collection<ItemStack> getDisplayItems();

    @Accessor("displayItemsSearchTab")
    Set<ItemStack> getDisplayItemsSearchTab();
}